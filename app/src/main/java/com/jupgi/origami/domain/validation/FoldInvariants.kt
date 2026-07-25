package com.jupgi.origami.domain.validation

import com.jupgi.origami.domain.model.FoldAssignment
import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3
import kotlin.math.abs

/** 위반 종류. 위쪽은 "접힌 결과"의 문제, 아래쪽은 "스텝 데이터 인코딩"의 문제. */
enum class InvariantKind {
    /** 정점/면 개수가 변했다 — 종이가 찢어지거나 생겨났다. */
    TOPOLOGY_CHANGED,

    /** 변 길이가 변했다 — 종이가 늘어났다. 스텝 인코딩 오류의 가장 흔한 증상. */
    EDGE_LENGTH_CHANGED,

    /** 삼각형 면적이 변했다. */
    FACE_AREA_CHANGED,

    /** 좌표에 NaN/Infinity가 나왔다. */
    NON_FINITE,

    /** progress=0인데 base와 다르다. */
    NOT_REVERSIBLE,

    /** progress를 조금 움직였는데 정점이 크게 튀었다. */
    DISCONTINUOUS,

    /** 힌지 두 끝점이 같아 회전축을 정의할 수 없다. */
    DEGENERATE_HINGE,

    /** 움직이는 정점이 하나도 없다 — 아무 일도 일어나지 않는 스텝. */
    EMPTY_MOVING_SET,

    /** movingVertexIndices가 정점 범위를 벗어났다. */
    VERTEX_INDEX_OUT_OF_RANGE,

    /** |foldAngleDeg| > 180 — 종이는 그 이상 접히지 않는다. */
    FOLD_ANGLE_OUT_OF_RANGE,

    /** 접힘각 부호와 산/계곡 배정이 모순이다(양수=계곡, 음수=산). */
    ASSIGNMENT_SIGN_MISMATCH,

    /**
     * 변의 한쪽 끝만 움직이는데 고정된 끝이 힌지 위에 없다 — 접는 선이 지나가는 자리에 정점이
     * 없어서 변이 꺾이지 못하고 통째로 끌려간다. **접어보기 전에** 잡히는 인코딩 오류로,
     * 해법은 힌지 선을 따라 메시를 분할하는 것이다.
     */
    EDGE_CROSSES_HINGE_UNSPLIT,
}

/**
 * @param magnitude 위반 정도(길이/면적 편차 등). 같은 종류끼리 "가장 나쁜 것"을 고르는 데 쓴다.
 */
data class InvariantViolation(
    val kind: InvariantKind,
    val detail: String,
    val magnitude: Float,
)

/**
 * 폴딩 결과가 **실제 종이처럼 접혔는지** 검사하는 순수 함수 모음.
 *
 * `FoldMeshAtUseCase`의 유닛테스트는 회전 수학이 맞는지만 본다. 하지만 스텝 데이터
 * (힌지 위치·`movingVertexIndices`)를 잘못 쓰면 수학은 맞는데 **종이가 늘어나거나 찢어진다.**
 * 그 구멍을 막는 것이 이 파일이다 — `.claude/rules/fold-model.md`의 불변식을 코드로 고정한다.
 *
 * 핵심 원리: 접기는 강체 회전이므로 **변 길이와 면적은 절대 변하지 않는다.** 변 하나의 두 정점 중
 * 한쪽만 움직이면서 고정된 쪽이 힌지 위에 없으면 그 변은 늘어나고, 여기서 바로 잡힌다.
 */
object FoldInvariants {
    /** 좌표 범위가 [-1,1]인 종이 기준. 누적 회전의 부동소수 오차를 감안한 값. */
    const val DEFAULT_LENGTH_TOLERANCE: Float = 1e-3f

    /** 종이는 180°를 넘겨 접히지 않는다. */
    const val MAX_FOLD_ANGLE_DEG: Float = 180f

    /** 기본 샘플 밀도 — 스텝당 20회면 180° 회전에서 9°마다 검사한다. */
    const val DEFAULT_SAMPLES_PER_STEP: Int = 20

    /**
     * 기본 점프 허용치. 반지름 2, 스텝당 20샘플이면 한 샘플의 이론 최대 이동은
     * 2·π/20 ≈ 0.31 이므로 여유를 둔 값.
     */
    const val DEFAULT_MAX_VERTEX_JUMP: Float = 0.5f

    /**
     * [folded]가 [base]를 강체 접기한 결과로서 타당한지 검사한다.
     * 위상이 깨졌으면 이후 검사가 무의미하므로 즉시 반환한다.
     */
    fun checkMesh(
        base: PaperMesh,
        folded: PaperMesh,
        tolerance: Float = DEFAULT_LENGTH_TOLERANCE,
    ): List<InvariantViolation> {
        if (base.vertices.size != folded.vertices.size || base.faces.size != folded.faces.size) {
            return listOf(
                InvariantViolation(
                    InvariantKind.TOPOLOGY_CHANGED,
                    "정점 ${base.vertices.size}→${folded.vertices.size}, " +
                        "면 ${base.faces.size}→${folded.faces.size}",
                    magnitude = 1f,
                ),
            )
        }
        val nonFinite =
            folded.vertices.mapIndexedNotNull { i, v ->
                if (isFinite(v)) {
                    null
                } else {
                    InvariantViolation(InvariantKind.NON_FINITE, "정점 $i = $v", magnitude = 1f)
                }
            }
        if (nonFinite.isNotEmpty()) return nonFinite

        return checkEdgeLengths(base, folded, tolerance) + checkFaceAreas(base, folded, tolerance)
    }

    /** 스텝 데이터 자체의 인코딩 규칙(`rules/fold-model.md`)을 검사한다. 접어보지 않아도 알 수 있는 것들. */
    fun checkStepEncoding(model: OrigamiModel): List<InvariantViolation> {
        val out = mutableListOf<InvariantViolation>()
        val vertexCount = model.base.vertices.size
        model.steps.forEachIndexed { i, step ->
            val where = "step[$i] '${step.id}'"
            if ((step.hingeEnd - step.hingeStart).length() < Vec3.EPS) {
                out += InvariantViolation(InvariantKind.DEGENERATE_HINGE, "$where 힌지 길이 0", 1f)
            }
            if (step.movingVertexIndices.isEmpty()) {
                out += InvariantViolation(InvariantKind.EMPTY_MOVING_SET, "$where 움직이는 정점 없음", 1f)
            }
            val outOfRange = step.movingVertexIndices.filter { it !in 0 until vertexCount }
            if (outOfRange.isNotEmpty()) {
                out +=
                    InvariantViolation(
                        InvariantKind.VERTEX_INDEX_OUT_OF_RANGE,
                        "$where 범위 밖 인덱스 $outOfRange (정점 수 $vertexCount)",
                        outOfRange.size.toFloat(),
                    )
            }
            val excess = abs(step.foldAngleDeg) - MAX_FOLD_ANGLE_DEG
            if (excess > 0f) {
                out +=
                    InvariantViolation(
                        InvariantKind.FOLD_ANGLE_OUT_OF_RANGE,
                        "$where 접힘각 ${step.foldAngleDeg}°",
                        excess,
                    )
            }
            val expected =
                if (step.foldAngleDeg >= 0f) FoldAssignment.VALLEY else FoldAssignment.MOUNTAIN
            if (step.assignment != expected) {
                out +=
                    InvariantViolation(
                        InvariantKind.ASSIGNMENT_SIGN_MISMATCH,
                        "$where 각 ${step.foldAngleDeg}°인데 ${step.assignment} (기대: $expected)",
                        1f,
                    )
            }
        }
        return out
    }

    /**
     * 각 스텝에서 **힌지 선을 따라 메시가 분할돼 있는지** 검사한다.
     *
     * 변 (u,v)의 한쪽만 움직이면, 고정된 쪽은 힌지 축 **위에** 있어야 한다(회전해도 제자리라
     * 길이가 보존됨). 그렇지 않으면 그 변은 늘어난다 — 실제 종이라면 접는 선에서 꺾여야 할
     * 자리에 정점이 없다는 뜻이다.
     *
     * 스텝 i의 힌지는 "직전 단계까지 접힌 좌표계"에서 유효해야 하므로 [foldAt]`(i)`를 기준으로 본다.
     */
    fun checkHingeSplitting(
        model: OrigamiModel,
        tolerance: Float = DEFAULT_LENGTH_TOLERANCE,
        foldAt: (Float) -> PaperMesh,
    ): List<InvariantViolation> {
        val edges = edgesOf(model.base)
        return model.steps.flatMapIndexed { i, step ->
            val before = foldAt(i.toFloat())
            val axisDir = (step.hingeEnd - step.hingeStart).normalized()
            if (before.vertices.size != model.base.vertices.size || axisDir.length() < Vec3.EPS) {
                emptyList()
            } else {
                edges.mapNotNull { (u, v) ->
                    hingeSplitViolation(before, step.movingVertexIndices, u, v, step, i, tolerance, axisDir)
                }
            }
        }
    }

    /**
     * progress를 0부터 stepCount까지 촘촘히 훑으며 매 지점에서 [checkMesh]를 돌린다.
     * 작품을 추가할 때마다 이 한 번으로 "실제로 접히는 작품인가"가 검증된다(LOOP-2 계약).
     *
     * @param foldAt progress → 접힌 메시. `FoldMeshAtUseCase`를 넘긴다(도메인 순수성 유지).
     * @param maxVertexJump 인접 샘플 간 허용 최대 정점 이동. 초과하면 불연속(점프)으로 본다.
     * @return 종류별 **최악** 위반만 (같은 종류가 수백 개 쏟아지는 것을 막는다).
     */
    fun sweep(
        model: OrigamiModel,
        samplesPerStep: Int = DEFAULT_SAMPLES_PER_STEP,
        tolerance: Float = DEFAULT_LENGTH_TOLERANCE,
        maxVertexJump: Float = DEFAULT_MAX_VERTEX_JUMP,
        foldAt: (Float) -> PaperMesh,
    ): List<InvariantViolation> {
        val found = mutableListOf<InvariantViolation>()
        found += checkStepEncoding(model)
        found += checkHingeSplitting(model, tolerance, foldAt)

        val totalSamples = model.stepCount * samplesPerStep
        var previous: PaperMesh? = null
        for (s in 0..totalSamples) {
            val progress = s.toFloat() / samplesPerStep
            val mesh = foldAt(progress)
            found +=
                checkMesh(model.base, mesh, tolerance).map {
                    it.copy(detail = "progress=%.3f · %s".format(progress, it.detail))
                }
            if (s == 0) found += reversibilityViolation(model.base, mesh, tolerance)
            found += jumpViolation(previous, mesh, maxVertexJump, progress)
            previous = mesh
        }
        return worstPerKind(found)
    }

    private fun checkEdgeLengths(
        base: PaperMesh,
        folded: PaperMesh,
        tolerance: Float,
    ): List<InvariantViolation> =
        edgesOf(base).mapNotNull { (u, v) ->
            val before = (base.vertices[u] - base.vertices[v]).length()
            val after = (folded.vertices[u] - folded.vertices[v]).length()
            val deviation = abs(after - before)
            if (deviation <= tolerance) {
                null
            } else {
                InvariantViolation(
                    InvariantKind.EDGE_LENGTH_CHANGED,
                    "변($u,$v) 길이 $before → $after (종이가 늘어났다)",
                    deviation,
                )
            }
        }

    private fun checkFaceAreas(
        base: PaperMesh,
        folded: PaperMesh,
        tolerance: Float,
    ): List<InvariantViolation> =
        base.faces.mapIndexedNotNull { i, face ->
            val before = triangleArea(base, face.a, face.b, face.c)
            val after = triangleArea(folded, face.a, face.b, face.c)
            val deviation = abs(after - before)
            if (deviation <= tolerance) {
                null
            } else {
                InvariantViolation(
                    InvariantKind.FACE_AREA_CHANGED,
                    "면[$i] 면적 $before → $after",
                    deviation,
                )
            }
        }
}

// ── 파일 레벨 순수 헬퍼 (object 표면을 좁게 유지한다) ──────────────────────────────

@Suppress("LongParameterList")
private fun hingeSplitViolation(
    before: PaperMesh,
    moving: Set<Int>,
    u: Int,
    v: Int,
    step: com.jupgi.origami.domain.model.FoldStep,
    stepIndex: Int,
    tolerance: Float,
    axisDir: Vec3,
): InvariantViolation? {
    val uMoving = u in moving
    if (uMoving == (v in moving)) return null
    val fixed = if (uMoving) v else u
    val distance = distanceToAxis(before.vertices[fixed], step.hingeStart, axisDir)
    if (distance <= tolerance) return null
    return InvariantViolation(
        InvariantKind.EDGE_CROSSES_HINGE_UNSPLIT,
        "step[$stepIndex] '${step.id}' 변($u,$v): 고정단 $fixed 이 힌지에서 $distance 떨어져 있다 " +
            "(힌지 선을 따라 메시를 분할해야 한다)",
        distance,
    )
}

private fun reversibilityViolation(
    base: PaperMesh,
    mesh: PaperMesh,
    tolerance: Float,
): List<InvariantViolation> {
    val (index, delta) = maxVertexDelta(base, mesh) ?: return emptyList()
    if (delta <= tolerance) return emptyList()
    return listOf(
        InvariantViolation(
            InvariantKind.NOT_REVERSIBLE,
            "progress=0인데 정점 $index 이 base와 $delta 만큼 다름",
            delta,
        ),
    )
}

private fun jumpViolation(
    previous: PaperMesh?,
    mesh: PaperMesh,
    maxVertexJump: Float,
    progress: Float,
): List<InvariantViolation> {
    val prev = previous ?: return emptyList()
    val (index, delta) = maxVertexDelta(prev, mesh) ?: return emptyList()
    if (delta <= maxVertexJump) return emptyList()
    return listOf(
        InvariantViolation(
            InvariantKind.DISCONTINUOUS,
            "progress=%.3f 부근 정점 %d 이 한 샘플에 %f 이동".format(progress, index, delta),
            delta,
        ),
    )
}

/** 종류별로 magnitude가 가장 큰 것만 남기고 내림차순 정렬한다. */
private fun worstPerKind(all: List<InvariantViolation>): List<InvariantViolation> =
    all
        .groupBy { it.kind }
        .values
        .mapNotNull { group -> group.maxByOrNull { it.magnitude } }
        .sortedByDescending { it.magnitude }

/** 삼각형 면에서 무향 변 집합을 뽑는다(작은 인덱스 먼저 → 중복 제거). */
private fun edgesOf(mesh: PaperMesh): Set<Pair<Int, Int>> {
    val edges = HashSet<Pair<Int, Int>>(mesh.faces.size * 2)
    for (f in mesh.faces) {
        edges += ordered(f.a, f.b)
        edges += ordered(f.b, f.c)
        edges += ordered(f.c, f.a)
    }
    return edges
}

private fun ordered(
    a: Int,
    b: Int,
): Pair<Int, Int> = if (a <= b) a to b else b to a

private fun triangleArea(
    mesh: PaperMesh,
    a: Int,
    b: Int,
    c: Int,
): Float {
    val p0 = mesh.vertices[a]
    return HALF * (mesh.vertices[b] - p0).cross(mesh.vertices[c] - p0).length()
}

/** 점에서 (axisPoint, 단위 axisDir) 직선까지의 수직 거리. */
private fun distanceToAxis(
    p: Vec3,
    axisPoint: Vec3,
    axisDir: Vec3,
): Float {
    val w = p - axisPoint
    return (w - axisDir * w.dot(axisDir)).length()
}

/** 두 메시에서 가장 많이 움직인 정점 (인덱스, 거리). 정점 수가 다르면 null. */
private fun maxVertexDelta(
    a: PaperMesh,
    b: PaperMesh,
): Pair<Int, Float>? {
    if (a.vertices.size != b.vertices.size) return null
    var bestIndex = 0
    var best = 0f
    a.vertices.forEachIndexed { i, v ->
        val d = (b.vertices[i] - v).length()
        if (d > best) {
            best = d
            bestIndex = i
        }
    }
    return bestIndex to best
}

private fun isFinite(v: Vec3): Boolean = v.x.isFinite() && v.y.isFinite() && v.z.isFinite()

private const val HALF = 0.5f

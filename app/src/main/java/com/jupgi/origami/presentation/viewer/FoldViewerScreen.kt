package com.jupgi.origami.presentation.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jupgi.origami.R
import com.jupgi.origami.domain.model.Face
import com.jupgi.origami.domain.model.FoldAssignment
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin

@Composable
fun FoldViewerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: FoldViewerViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 종이를 접는 동안 화면이 꺼지면 안 된다 — 두 손이 종이에 묶여 있다.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize()
                        // 상태바·내비게이션바와 겹치지 않게(enableEdgeToEdge 대응).
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Header(title = state.title, difficulty = state.difficulty, onBack = onBack)

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    PaperCanvas(
                        state = state,
                        onTogglePlay = viewModel::togglePlay,
                        onPrev = viewModel::prevStep,
                        onNext = viewModel::nextStep,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                StepInfo(
                    stepNumber = state.currentStepNumber,
                    stepCount = state.stepCount,
                    instruction = state.currentStep?.instruction.orEmpty(),
                )

                Slider(
                    value = state.progress,
                    onValueChange = viewModel::onProgressChange,
                    valueRange = 0f..max(state.stepCount, 1).toFloat(),
                )

                Controls(
                    isPlaying = state.isPlaying,
                    onPrev = viewModel::prevStep,
                    onTogglePlay = viewModel::togglePlay,
                    onNext = viewModel::nextStep,
                )

                Text(
                    text = stringResource(R.string.viewer_drag_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun Header(
    title: String,
    difficulty: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(
            text = stringResource(R.string.viewer_difficulty_label, difficulty),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun StepInfo(
    stepNumber: Int,
    stepCount: Int,
    instruction: String,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = stringResource(R.string.viewer_step_label, stepNumber, stepCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        if (instruction.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(text = instruction, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Controls(
    isPlaying: Boolean,
    onPrev: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.viewer_prev_step))
        }
        Button(onClick = onTogglePlay, modifier = Modifier.weight(1.2f)) {
            Text(
                stringResource(
                    if (isPlaying) R.string.viewer_pause else R.string.viewer_play,
                ),
            )
        }
        OutlinedButton(onClick = onNext, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.viewer_next_step))
        }
    }
}

/**
 * 도메인이 계산한 메시를 Compose Canvas 로 투영해 그리는 소프트웨어 3D 렌더러.
 * 화가 알고리즘(깊이 정렬) + 램버트 음영 + 드래그 카메라 오빗. 도메인은 렌더러와 완전히 분리돼
 * 있어, M2 에서 이 컴포저블만 SceneView(Filament)로 교체하면 된다(docs/ARCHITECTURE.md).
 *
 * 터치 규약: 재생 중엔 아무 데나 탭 = 일시정지(OriSim3D 패턴), 멈춰 있으면 화면 좌/우 절반 탭 =
 * 이전/다음 단계. 종이를 든 손으로도 누를 수 있게 타겟을 화면 절반으로 크게 잡았다.
 */
@Composable
private fun PaperCanvas(
    state: FoldViewerUiState,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var yaw by remember { mutableFloatStateOf(0.5f) }
    var pitch by remember { mutableFloatStateOf(-0.55f) }
    var zoom by remember { mutableFloatStateOf(1f) }

    val creaseColor = MaterialTheme.colorScheme.primary
    val isPlaying = state.isPlaying

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    // 한 손가락 = 카메라 오빗, 두 손가락 = 확대/축소. 배율은 사용자가 정한다
                    // (자동 줌은 접힐 때마다 배율이 바뀌어 오히려 혼란스럽다).
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            yaw += pan.x * DRAG_SENSITIVITY
                            pitch = (pitch + pan.y * DRAG_SENSITIVITY).coerceIn(-PITCH_LIMIT, PITCH_LIMIT)
                            zoom = (zoom * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        }
                    }.pointerInput(isPlaying) {
                        detectTapGestures { offset ->
                            when {
                                isPlaying -> onTogglePlay()
                                offset.x < size.width / 2f -> onPrev()
                                else -> onNext()
                            }
                        }
                    },
        ) {
            val mesh = state.mesh ?: return@Canvas
            val camVerts = mesh.vertices.map { rotateCamera(it, yaw, pitch) }
            val projection = fixedProjection(size, zoom)
            drawPaper(mesh, camVerts, state.layerOrder, state.flipParity, projection)
            val step = state.currentStep
            if (step != null) {
                val a = rotateCamera(step.hingeStart, yaw, pitch)
                val b = rotateCamera(step.hingeEnd, yaw, pitch)
                val isValley = step.assignment != FoldAssignment.MOUNTAIN
                drawCrease(projection.toScreen(a), projection.toScreen(b), creaseColor, isValley)
            }
        }
    }
}

/** 모델 좌표 → 화면 좌표 변환. */
private data class Projection(
    val scale: Float,
    val cx: Float,
    val cy: Float,
) {
    fun toScreen(v: Vec3): Offset = Offset(cx + v.x * scale, cy - v.y * scale)
}

/**
 * 펼친 종이 기준 **고정 배율** × 사용자 [zoom]. 화면 중앙 정렬.
 *
 * 접힌 크기에 맞춰 자동으로 확대하면 단계마다 배율이 달라져 "종이가 작아졌다/커졌다"가 뒤섞여
 * 오히려 방향 감각을 잃는다. 배율은 사용자가 핀치로 정한다.
 */
private fun fixedProjection(
    size: Size,
    zoom: Float,
): Projection =
    Projection(
        scale = min(size.width, size.height) * BASE_FIT_RATIO / MODEL_RADIUS * zoom,
        cx = size.width / 2f,
        cy = size.height / 2f,
    )

private fun DrawScope.drawPaper(
    mesh: PaperMesh,
    camVerts: List<Vec3>,
    layerOrder: List<Int>,
    flipParity: List<Boolean>,
    projection: Projection,
) {
    // ── 겹 단위 렌더링 + 기하 오프셋 ─────────────────────────────────────────
    //
    // 1) 같은 겹의 삼각형들을 하나의 Path 로 묶어 한 번에 칠한다 — 한 겹은 강체 변환을 함께
    //    받아 항상 한 평면이라 색·음영이 하나이고, 겹 안에서 순서가 뒤집힐 여지가 없다.
    // 2) 각 겹을 **쌓임 방향으로 `겹 × 종이두께` 만큼 실제로 띄운다.** 겹친 종이가 동일평면인
    //    한 어떤 정렬 규칙도 보는 방향에 따라 틀릴 수 있다(이전의 화면 방향 휴리스틱이 접는
    //    중이나 옆 각도에서 어긋나던 원인). 오프셋을 주면 깊이가 실제로 갈려 **순수 깊이
    //    정렬만으로 어느 각도에서든 옳다** — OpenGL polygon offset·D3D depth bias·데칼
    //    렌더링과 같은 계열의 업계 표준 기법이고, 실제 종이도 두께가 있으니 물리적으로도 맞다.
    //    쌓임 방향 = 면 법선 × (뒤집힌 면이면 -1) — 뒤집힌 면은 법선이 스택 반대를 향한다.
    //    도메인 기하는 불변이므로(렌더 시점만) 이후 힌지 계산에 영향이 없다.
    val groups =
        mesh.faces.indices.groupBy {
            layerOrder.getOrElse(it) { 0 } to (faceNormal(camVerts, mesh.faces[it]).z >= 0f)
        }

    val shells =
        groups.map { (key, faceIndices) ->
            val representative = mesh.faces[faceIndices.first()]
            val normal = faceNormal(camVerts, representative)
            val flipped = flipParity.getOrElse(faceIndices.first()) { false }
            val stackUp = if (flipped) normal * -1f else normal
            val offset = stackUp * (key.first * PAPER_THICKNESS)
            val depth =
                faceIndices
                    .map { i ->
                        val f = mesh.faces[i]
                        (camVerts[f.a].z + camVerts[f.b].z + camVerts[f.c].z) / 3f
                    }.average()
                    .toFloat() + offset.z
            Shell(faceIndices, offset, depth, normal)
        }

    for (shell in shells.sortedBy { it.depth }) {
        // 종이는 양면 — 실제 색종이처럼 앞은 색, 뒤는 흰색. 배경과 섞이지 않게 테마와 독립적으로 둔다.
        val base = if (shell.normal.z >= 0f) PAPER_FRONT else PAPER_BACK
        val lambert = abs(shell.normal.dot(LIGHT_DIR))
        val brightness = (AMBIENT + (1f - AMBIENT) * lambert).coerceIn(0f, 1f)
        val shaded = Color(base.red * brightness, base.green * brightness, base.blue * brightness, 1f)

        val path = Path()
        for (index in shell.faceIndices) {
            val face = mesh.faces[index]
            val s0 = projection.toScreen(camVerts[face.a] + shell.offset)
            val s1 = projection.toScreen(camVerts[face.b] + shell.offset)
            val s2 = projection.toScreen(camVerts[face.c] + shell.offset)
            path.moveTo(s0.x, s0.y)
            path.lineTo(s1.x, s1.y)
            path.lineTo(s2.x, s2.y)
            path.close()
        }
        drawPath(path, color = shaded)
    }
}

/** 한 겹(같은 layer·같은 방향)의 렌더 단위 — 오프셋과 깊이를 함께 든다. */
private data class Shell(
    val faceIndices: List<Int>,
    val offset: Vec3,
    val depth: Float,
    val normal: Vec3,
)

private fun DrawScope.drawCrease(
    start: Offset,
    end: Offset,
    color: Color,
    isValley: Boolean,
) {
    // 계곡=파선, 산=일점쇄선(요시자와·랜들렛 규약, docs/DESIGN.md).
    val effect =
        if (isValley) {
            PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
        } else {
            PathEffect.dashPathEffect(floatArrayOf(22f, 8f, 4f, 8f))
        }
    drawLine(color = color, start = start, end = end, strokeWidth = 5f, pathEffect = effect)
}

/** yaw(Y축) → pitch(X축) 순으로 카메라 회전. 결과 z 가 클수록 뷰어에 가깝다. */
private fun rotateCamera(
    v: Vec3,
    yaw: Float,
    pitch: Float,
): Vec3 {
    val cy = cos(yaw)
    val sy = sin(yaw)
    val x1 = v.x * cy + v.z * sy
    val z1 = -v.x * sy + v.z * cy
    val cp = cos(pitch)
    val sp = sin(pitch)
    val y2 = v.y * cp - z1 * sp
    val z2 = v.y * sp + z1 * cp
    return Vec3(x1, y2, z2)
}

/** 면의 법선(카메라 좌표계). 한 겹은 한 평면이라 대표 면 하나로 겹 전체의 향을 정할 수 있다. */
private fun faceNormal(
    camVerts: List<Vec3>,
    face: Face,
): Vec3 {
    val p0 = camVerts[face.a]
    return (camVerts[face.b] - p0).cross(camVerts[face.c] - p0).normalized()
}

private const val DRAG_SENSITIVITY = 0.01f
private const val PITCH_LIMIT = 1.4f

/** 펼친 종이가 캔버스에서 차지할 비율(줌 1배 기준). */
private const val BASE_FIT_RATIO = 0.42f

/** 종이 반지름 여유(펼친 정사각형의 대각 반지름 √2 보다 조금 크게). */
private const val MODEL_RADIUS = 1.5f

private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 6f

/**
 * 그림자 쪽 최소 밝기. 0.55 에서는 크림색 뒷면이 진회색으로 보여 "다른 색"으로 오인됐다 —
 * 이 앱에서 음영은 입체감 힌트일 뿐, 종이의 앞/뒤 식별(주황/크림)이 항상 우선이다.
 */
private const val AMBIENT = 0.78f

/**
 * 겹당 종이 두께(모델 좌표, 종이 한 변 = 2). 렌더 시점에 겹을 쌓임 방향으로 이만큼 띄워
 * 동일평면 z-fighting 을 원천 제거한다 — polygon offset/데칼 계열의 표준 기법.
 * 화면(기본 줌)에서 겹당 약 1px, 접힌 모서리에서 도톰한 종이 느낌.
 */
private const val PAPER_THICKNESS = 0.008f

/** 색종이 앞면 — 따뜻한 주황. */
private val PAPER_FRONT = Color(0xFFE8703A)

/** 색종이 뒷면 — 실제 색종이처럼 크림 화이트. 어두운 배경과 확실히 대비된다. */
private val PAPER_BACK = Color(0xFFF7F2E8)

private val LIGHT_DIR = Vec3(0.3f, 0.5f, 1.0f).normalized()

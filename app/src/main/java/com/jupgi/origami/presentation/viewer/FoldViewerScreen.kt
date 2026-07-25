package com.jupgi.origami.presentation.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Button
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
import com.jupgi.origami.domain.model.FoldAssignment
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun FoldViewerScreen(modifier: Modifier = Modifier) {
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
                Header(title = state.title, difficulty = state.difficulty)

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    PaperCanvas(
                        mesh = state.mesh,
                        creaseStart = state.currentStep?.hingeStart,
                        creaseEnd = state.currentStep?.hingeEnd,
                        creaseIsValley = state.currentStep?.assignment != FoldAssignment.MOUNTAIN,
                        isPlaying = state.isPlaying,
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
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
    mesh: PaperMesh?,
    creaseStart: Vec3?,
    creaseEnd: Vec3?,
    creaseIsValley: Boolean,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var yaw by remember { mutableFloatStateOf(0.5f) }
    var pitch by remember { mutableFloatStateOf(-0.55f) }

    val creaseColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            yaw += drag.x * DRAG_SENSITIVITY
                            pitch = (pitch + drag.y * DRAG_SENSITIVITY).coerceIn(-PITCH_LIMIT, PITCH_LIMIT)
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
            if (mesh == null) return@Canvas
            val camVerts = mesh.vertices.map { rotateCamera(it, yaw, pitch) }
            val projection = fitProjection(camVerts, size)
            drawPaper(mesh, camVerts, projection)
            if (creaseStart != null && creaseEnd != null) {
                val a = rotateCamera(creaseStart, yaw, pitch)
                val b = rotateCamera(creaseEnd, yaw, pitch)
                drawCrease(projection.toScreen(a), projection.toScreen(b), creaseColor, creaseIsValley)
            }
        }
    }
}

/** 모델 좌표 → 화면 좌표 변환. 매 프레임 메시 크기에 맞춰 다시 계산된다(자동 줌). */
private data class Projection(
    val scale: Float,
    val cx: Float,
    val cy: Float,
) {
    fun toScreen(v: Vec3): Offset = Offset(cx + v.x * scale, cy - v.y * scale)
}

/**
 * 접힌 메시의 화면 투영 바운딩 박스에 맞춰 확대·중앙 정렬한다.
 * 고정 배율이면 접을수록 종이가 작아져 보기 어렵다.
 */
private fun fitProjection(
    camVerts: List<Vec3>,
    size: Size,
): Projection {
    if (camVerts.isEmpty()) return Projection(1f, size.width / 2f, size.height / 2f)
    val minX = camVerts.minOf { it.x }
    val maxX = camVerts.maxOf { it.x }
    val minY = camVerts.minOf { it.y }
    val maxY = camVerts.maxOf { it.y }
    val spanX = (maxX - minX).coerceAtLeast(MIN_SPAN)
    val spanY = (maxY - minY).coerceAtLeast(MIN_SPAN)
    val scale = min(size.width / spanX, size.height / spanY) * FIT_RATIO
    return Projection(
        scale = scale,
        cx = size.width / 2f - (minX + maxX) / 2f * scale,
        cy = size.height / 2f + (minY + maxY) / 2f * scale,
    )
}

private fun DrawScope.drawPaper(
    mesh: PaperMesh,
    camVerts: List<Vec3>,
    projection: Projection,
) {
    // 화가 알고리즘: 깊이(z) 오름차순 = 먼 면부터 그린다(뷰어는 +Z).
    //
    // 깊이를 **양자화**하는 이유: 반으로 접으면 겹친 레이어가 동일평면이 되어 centroid z 가
    // 부동소수 오차 수준으로만 갈린다. 그대로 정렬하면 삼각형마다 이기는 레이어가 뒤바뀌어
    // 앞면/뒷면이 뒤섞인 체커보드가 된다. 양자화하면 그 면들이 동점이 되고, Kotlin 의 안정
    // 정렬이 원래 면 순서를 유지해 **결정적이고 일관된** 그림이 나온다.
    // (정확한 겹 순서는 FOLD faceOrders 가 필요 — M2.)
    val ordered =
        mesh.faces.sortedBy { f ->
            val z = (camVerts[f.a].z + camVerts[f.b].z + camVerts[f.c].z) / 3f
            kotlin.math.round(z / DEPTH_QUANTUM)
        }

    for (face in ordered) {
        val p0 = camVerts[face.a]
        val p1 = camVerts[face.b]
        val p2 = camVerts[face.c]
        val normal = (p1 - p0).cross(p2 - p0).normalized()
        // 종이는 양면 — 실제 색종이처럼 앞은 색, 뒤는 흰색. 배경과 섞이지 않게 테마와 독립적으로 둔다.
        val base = if (normal.z >= 0f) PAPER_FRONT else PAPER_BACK
        val lambert = abs(normal.dot(LIGHT_DIR))
        val brightness = (AMBIENT + (1f - AMBIENT) * lambert).coerceIn(0f, 1f)
        val shaded = Color(base.red * brightness, base.green * brightness, base.blue * brightness, 1f)

        val path =
            Path().apply {
                val s0 = projection.toScreen(p0)
                val s1 = projection.toScreen(p1)
                val s2 = projection.toScreen(p2)
                moveTo(s0.x, s0.y)
                lineTo(s1.x, s1.y)
                lineTo(s2.x, s2.y)
                close()
            }
        drawPath(path, color = shaded)
    }
}

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

private const val DRAG_SENSITIVITY = 0.01f
private const val PITCH_LIMIT = 1.4f

/** 캔버스 대비 종이가 차지할 비율(여백 확보). */
private const val FIT_RATIO = 0.86f

/** 0으로 나누기 방지 — 종이가 정확히 옆에서 보여 두께가 0이 될 때. */
private const val MIN_SPAN = 0.05f

/** 그림자 쪽 최소 밝기(완전히 검어지지 않게). */
private const val AMBIENT = 0.55f

/**
 * 깊이 정렬 양자화 폭(모델 좌표, 종이 한 변이 2). 이보다 가까운 면들은 동점으로 보고
 * 원래 면 순서를 유지한다 — 동일평면 레이어에서 정렬이 요동치는 것을 막는다.
 */
private const val DEPTH_QUANTUM = 0.02f

/** 색종이 앞면 — 따뜻한 주황. */
private val PAPER_FRONT = Color(0xFFE8703A)

/** 색종이 뒷면 — 실제 색종이처럼 크림 화이트. 어두운 배경과 확실히 대비된다. */
private val PAPER_BACK = Color(0xFFF7F2E8)

private val LIGHT_DIR = Vec3(0.3f, 0.5f, 1.0f).normalized()

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
import kotlin.math.round
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
            // 겹이 쌓이는 방향(base +Z)이 화면 반대쪽을 향하면 레이어 순서를 뒤집는다.
            val flipLayers = rotateCamera(STACK_UP, yaw, pitch).z < 0f
            drawPaper(mesh, camVerts, state.layerOrder, flipLayers, projection)
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
    flipLayers: Boolean,
    projection: Projection,
) {
    // 화가 알고리즘: 먼 것부터 그린다. 키가 두 개인 이유 —
    //
    // 1) 깊이(z)를 **양자화**: 반으로 접으면 겹친 레이어가 동일평면이 되어 centroid z 가
    //    부동소수 오차 수준으로만 갈린다. 그대로 정렬하면 삼각형마다 이기는 면이 달라져
    //    앞/뒷면이 뒤섞인 체커보드가 된다. 양자화하면 그 면들이 동점이 된다.
    // 2) 동점은 **겹 순서**(ComputeLayerOrderUseCase)로 가린다. 깊이만으로는 알 수 없는
    //    "어느 종이가 위인가"를 접기 이력에서 계산한 값이다. 뒤에서 보면 순서가 뒤집힌다.
    val indices = mesh.faces.indices.sortedWith(compareBy({ depthKey(mesh, camVerts, it) }, { layerKey(layerOrder, it, flipLayers) }))

    for (faceIndex in indices) {
        val face = mesh.faces[faceIndex]
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

/** 깊이 정렬 1차 키 — 양자화해 동일평면 레이어를 동점으로 만든다. */
private fun depthKey(
    mesh: PaperMesh,
    camVerts: List<Vec3>,
    faceIndex: Int,
): Float {
    val f = mesh.faces[faceIndex]
    return round((camVerts[f.a].z + camVerts[f.b].z + camVerts[f.c].z) / 3f / DEPTH_QUANTUM)
}

/** 깊이 정렬 2차 키 — 겹 순서. 종이 뒤에서 보면 뒤집힌다. */
private fun layerKey(
    layerOrder: List<Int>,
    faceIndex: Int,
    flip: Boolean,
): Int {
    val layer = layerOrder.getOrElse(faceIndex) { 0 }
    return if (flip) -layer else layer
}

private const val DRAG_SENSITIVITY = 0.01f
private const val PITCH_LIMIT = 1.4f

/** 펼친 종이가 캔버스에서 차지할 비율(줌 1배 기준). */
private const val BASE_FIT_RATIO = 0.42f

/** 종이 반지름 여유(펼친 정사각형의 대각 반지름 √2 보다 조금 크게). */
private const val MODEL_RADIUS = 1.5f

private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 6f

/** 겹이 쌓이는 방향(펼친 종이의 앞면 법선). 카메라가 반대편이면 레이어 순서를 뒤집는다. */
private val STACK_UP = Vec3(0f, 0f, 1f)

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

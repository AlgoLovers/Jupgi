package com.jupgi.origami.presentation.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jupgi.origami.R
import com.jupgi.origami.domain.model.FoldAssignment
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun FoldViewerScreen(modifier: Modifier = Modifier) {
    val viewModel: FoldViewerViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Header(title = state.title, difficulty = state.difficulty)

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    PaperCanvas(
                        mesh = state.mesh,
                        creaseStart = state.currentStep?.hingeStart,
                        creaseEnd = state.currentStep?.hingeEnd,
                        creaseIsValley = state.currentStep?.assignment != FoldAssignment.MOUNTAIN,
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
                    onPrev = viewModel::prevStep,
                    onReset = viewModel::reset,
                    onNext = viewModel::nextStep,
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.viewer_drag_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.viewer_step_label, stepNumber, stepCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        if (instruction.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(text = instruction, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Controls(
    onPrev: () -> Unit,
    onReset: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onPrev, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.viewer_prev_step))
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.viewer_reset))
        }
        Button(onClick = onNext, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.viewer_next_step))
        }
    }
}

/**
 * 도메인이 계산한 메시를 Compose Canvas 로 투영해 그리는 소프트웨어 3D 렌더러.
 * 화가 알고리즘(깊이 정렬) + 램버트 음영 + 드래그 카메라 오빗. 도메인은 렌더러와 완전히 분리돼
 * 있어, M2 에서 이 컴포저블만 SceneView(Filament)로 교체하면 된다(docs/ARCHITECTURE.md).
 */
@Composable
private fun PaperCanvas(
    mesh: PaperMesh?,
    creaseStart: Vec3?,
    creaseEnd: Vec3?,
    creaseIsValley: Boolean,
    modifier: Modifier = Modifier,
) {
    var yaw by remember { mutableFloatStateOf(0.5f) }
    var pitch by remember { mutableFloatStateOf(-0.55f) }

    val frontColor = MaterialTheme.colorScheme.primary
    val backColor = MaterialTheme.colorScheme.surface
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
                    },
        ) {
            if (mesh == null) return@Canvas
            drawPaper(mesh, yaw, pitch, frontColor, backColor)
            if (creaseStart != null && creaseEnd != null) {
                drawCrease(creaseStart, creaseEnd, yaw, pitch, creaseColor, creaseIsValley)
            }
        }
    }
}

private fun DrawScope.drawPaper(
    mesh: PaperMesh,
    yaw: Float,
    pitch: Float,
    frontColor: Color,
    backColor: Color,
) {
    val scale = minOf(size.width, size.height) * 0.42f / MODEL_RADIUS
    val cx = size.width / 2f
    val cy = size.height / 2f
    val camVerts = mesh.vertices.map { rotateCamera(it, yaw, pitch) }

    // 화가 알고리즘: 깊이(z) 오름차순 = 먼 면부터 그린다(뷰어는 +Z).
    val ordered =
        mesh.faces.sortedBy { f ->
            (camVerts[f.a].z + camVerts[f.b].z + camVerts[f.c].z) / 3f
        }

    for (face in ordered) {
        val p0 = camVerts[face.a]
        val p1 = camVerts[face.b]
        val p2 = camVerts[face.c]
        val normal = (p1 - p0).cross(p2 - p0).normalized()
        val facesViewer = normal.z >= 0f
        // 종이는 양면 — 법선·광원 각도로 램버트 음영, 앞/뒤 색 구분.
        val lambert = kotlin.math.abs(normal.dot(LIGHT_DIR))
        val brightness = (0.45f + 0.55f * lambert).coerceIn(0f, 1f)
        val base = if (facesViewer) frontColor else backColor
        val shaded = Color(base.red * brightness, base.green * brightness, base.blue * brightness, 1f)

        val path =
            Path().apply {
                moveTo(cx + p0.x * scale, cy - p0.y * scale)
                lineTo(cx + p1.x * scale, cy - p1.y * scale)
                lineTo(cx + p2.x * scale, cy - p2.y * scale)
                close()
            }
        drawPath(path, color = shaded)
    }
}

private fun DrawScope.drawCrease(
    start: Vec3,
    end: Vec3,
    yaw: Float,
    pitch: Float,
    color: Color,
    isValley: Boolean,
) {
    val scale = minOf(size.width, size.height) * 0.42f / MODEL_RADIUS
    val cx = size.width / 2f
    val cy = size.height / 2f
    val a = rotateCamera(start, yaw, pitch)
    val b = rotateCamera(end, yaw, pitch)
    val pa = Offset(cx + a.x * scale, cy - a.y * scale)
    val pb = Offset(cx + b.x * scale, cy - b.y * scale)
    // 계곡=파선, 산=일점쇄선(요시자와·랜들렛 규약, docs/DESIGN.md).
    val effect =
        if (isValley) {
            PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
        } else {
            PathEffect.dashPathEffect(floatArrayOf(22f, 8f, 4f, 8f))
        }
    drawLine(color = color, start = pa, end = pb, strokeWidth = 5f, pathEffect = effect)
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
private const val MODEL_RADIUS = 1.5f
private val LIGHT_DIR = Vec3(0.3f, 0.5f, 1.0f).normalized()

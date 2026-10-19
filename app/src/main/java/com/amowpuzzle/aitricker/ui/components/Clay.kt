package com.amowpuzzle.aitricker.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amowpuzzle.aitricker.ui.theme.AppColors
import com.amowpuzzle.aitricker.ui.theme.AppText
import com.amowpuzzle.aitricker.ui.theme.Clay
import com.amowpuzzle.aitricker.ui.theme.Radius
import com.amowpuzzle.aitricker.ui.theme.Space

/**
 * The Soft-3D "Clay" component set.
 *
 * Everything with a surface in the app is built from these five: [ClayCard], [ClayButton],
 * [ClayCircleButton], [ClayPill] and [ClayBand]. They own every shadow and every radius in
 * the redesign so that no screen sets one directly - which is the only way "every card feels
 * the same" actually holds. All measurements come from [Clay], [Radius] and the spacing scale.
 *
 * The claymorphic look is two shadows, never one: a wide soft ambient pass that seats the
 * surface on the cream page, and a tight darker pass just below it that reads as the card's
 * thickness. Compose's [shadow] takes a single elevation, so each clay surface stacks two.
 */

/** Darken a colour toward its own shade - the extrusion / riser colour. */
private fun Color.shade(factor: Float = Clay.SHADE_FACTOR): Color =
    Color(red * factor, green * factor, blue * factor, alpha)

/** Lift a colour toward white, for the light top edge that sells the material. */
private fun Color.lift(amount: Float): Color = lerp(this, Color.White, amount)

/**
 * A clay surface: two stacked shadows plus a light inner top edge. Everything card-shaped
 * in the app - panels, sheets, rows, the board frame - is this.
 *
 * @param tint the surface colour; chapter tint on the header and dock, near-white elsewhere.
 * @param small halves the shadow strength for chips and inline cards.
 */
@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    tint: Color = AppColors.Background,
    shape: Shape = Radius.cardShape,
    small: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    val ambient = if (small) Clay.ambientSmall else Clay.ambient
    val direct = if (small) Clay.directSmall else Clay.direct
    Box(
        modifier = modifier
            // Ambient: wide, faint, warm - seats the card on the page.
            .shadow(ambient, shape, clip = false, ambientColor = AppColors.ClayShadow, spotColor = AppColors.ClayShadow)
            // Direct: tight, offset down - the thickness of the card.
            .offset(y = Clay.directOffsetY)
            .shadow(direct, shape, clip = false, ambientColor = AppColors.ClayShadow, spotColor = AppColors.ClayShadow)
            .offset(y = -Clay.directOffsetY)
            .clip(shape)
            .background(tint)
            // The light inner edge along the top: a hairline lift of the tint toward white.
            .border(Clay.innerHighlight, tint.lift(Clay.HIGHLIGHT_ALPHA), shape)
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * A 3D push button: a top face resting on a coloured riser. Pressing travels the face down
 * onto the riser so the press is felt as compression, not just a tint change. This is the
 * primary action on every screen - it replaces the flat [PrimaryPillButton] recipe.
 */
@Composable
fun ClayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = AppColors.Accent,
    contentColor: Color = AppColors.Background,
    enabled: Boolean = true,
    height: Dp = 58.dp,
    shape: RoundedCornerShape = Radius.pillShape,
    style: TextStyle = AppText.Button
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val face = if (enabled) container else AppColors.SurfaceStrong
    val riser = face.shade()
    val travel by animateDpAsState(
        targetValue = if (pressed && enabled) Clay.pressTravel else 0.dp,
        animationSpec = tween(90),
        label = "press"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + Clay.riserHeight)
    ) {
        // The riser: the coloured side wall the face sits on. Stationary.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .align(Alignment.BottomCenter)
                .clip(shape)
                .background(riser)
        )
        // The top face: travels down under the press, revealing less riser.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .align(Alignment.TopCenter)
                .offset(y = travel)
                .clip(shape)
                .background(
                    Brush.verticalGradient(listOf(face.lift(0.12f), face))
                )
                .clickableFace(enabled, text, onClick, interaction),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = style,
                color = if (enabled) contentColor else AppColors.SlateMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * A round clay control: the dock buttons (hint, grid, undo, restart) and the settings gear.
 * [active] keeps it pressed-in and ringed for as long as what it controls is on, so a toggle
 * reads as a switch. Replaces the flat white [CircleIconButton].
 */
@Composable
fun ClayCircleButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    face: Color = AppColors.Background,
    active: Boolean = false,
    enabled: Boolean = true,
    badge: Int? = null,
    icon: @Composable (Color) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val riser = face.shade()
    val down = pressed || active
    val travel by animateDpAsState(
        targetValue = if (down && enabled) Clay.pressTravel else 0.dp,
        animationSpec = tween(90),
        label = "circlePress"
    )
    val tint = when {
        !enabled -> AppColors.SlateMuted
        active -> AppColors.AccentPressed
        else -> AppColors.Accent
    }

    Box(modifier = modifier.size(size + Space.l), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(size + Clay.riserHeight)) {
            Box(
                modifier = Modifier
                    .size(size)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(riser)
            )
            Box(
                modifier = Modifier
                    .size(size)
                    .align(Alignment.TopCenter)
                    .offset(y = travel)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(face.lift(0.14f), face)))
                    .then(
                        if (active) Modifier.border(2.5.dp, AppColors.Accent, CircleShape) else Modifier
                    )
                    .clickableFace(enabled, contentDescription, onClick, interaction),
                contentAlignment = Alignment.Center
            ) {
                icon(tint)
            }
        }
        if (badge != null) {
            BadgeBubble(
                count = badge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-1).dp, y = 1.dp)
            )
        }
    }
}

/**
 * A small clay chip for a count or a label - arrows left, difficulty, coins. Same two-shadow
 * recipe as [ClayCard] at chip strength, so a pill reads as the same material as a panel.
 */
@Composable
fun ClayPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = AppColors.Background,
    contentColor: Color = AppColors.Navy,
    style: TextStyle = AppText.Counter,
    contentDescription: String? = null,
    leading: @Composable (() -> Unit)? = null
) {
    val label = contentDescription
    ClayCard(
        modifier = modifier,
        tint = container,
        shape = Radius.pillShape,
        small = true,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (label != null) {
                Modifier.clearAndSetSemantics { this.contentDescription = label }
            } else {
                Modifier
            }
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(7.dp))
            }
            Text(text = text, style = style, color = contentColor)
        }
    }
}

/**
 * A tinted band with a soft downward gradient, used behind the game header and above the
 * dock so the top and bottom of a screen carry the chapter's colour. Not a card - it bleeds
 * to the screen edges - so it draws its own single soft shadow along its inner edge.
 */
@Composable
fun ClayBand(
    tint: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = 0.86f))))
            .drawBehind {
                // A faint inner top highlight, drawn rather than bordered so it can be soft.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.4f
                    )
                )
            }
    ) {
        content()
    }
}

/** Shared clickable for the pressable clay faces, so the ripple and label handling match. */
private fun Modifier.clickableFace(
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
    interaction: MutableInteractionSource
): Modifier = this.then(
    clickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        onClickLabel = label,
        onClick = onClick
    )
)

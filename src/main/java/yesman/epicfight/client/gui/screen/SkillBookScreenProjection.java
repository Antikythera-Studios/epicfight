package yesman.epicfight.client.gui.screen;

import org.joml.Matrix4f;

/// Scales the GUI orthographic projection instead of rebuilding it so the
/// original depth range and translation survive small-window GUI scaling.
final class SkillBookScreenProjection {
	private SkillBookScreenProjection() {
	}

	/// Returns a copy of [originalProjection] scaled in x and y so content laid
	/// out for [bookScale] fills a window still reporting [originalScale]. The z
	/// column and translation column are left untouched, preserving the caller's
	/// near/far planes and the GUI modelview translation; the input matrix is
	/// not mutated.
	static Matrix4f forScale(Matrix4f originalProjection, double originalScale, double bookScale) {
		float ratio = (float)(bookScale / originalScale);
		return new Matrix4f(originalProjection).scale(ratio, ratio, 1.0F);
	}
}

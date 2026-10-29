package yesman.epicfight.client.renderer.shader.compute;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryUtil;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.SkinnedMesh.SkinnedMeshPart;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.GLConstants;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.shader.compute.backend.buffers.MappedBuffer;
import yesman.epicfight.client.renderer.shader.compute.backend.program.ComputeProgram;
import yesman.epicfight.client.renderer.shader.compute.loader.ComputeShaderProvider;
import yesman.epicfight.config.ClientConfig;

import javax.annotation.Nullable;
import java.util.Arrays;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.glVertexAttribIPointer;

public class VanillaComputeShaderSetup extends ComputeShaderSetup {
	
	public VanillaComputeShaderSetup(SkinnedMesh skinnedMesh) {
        super(skinnedMesh, 12);
    }
	
	@Override
	public void bindBufferFormat(VertexFormat vertexFormat) {
		var elems = vertexFormat.getElements();
		glBindBuffer(GL_ARRAY_BUFFER, this.outVertexAttrBO.glSSBO);
		
		for (int i = 0; i < elems.size(); ++i) {
			VertexFormatElement elem = elems.get(i);
			
			if (elem == VertexFormatElement.POSITION) {
				glVertexAttribPointer(i, 3, GL_FLOAT, false, 48, 0);
				glEnableVertexAttribArray(i);
			} else if (elem == VertexFormatElement.UV0) {
				glVertexAttribPointer(i, 2, GL_FLOAT, false, 48, 28);
				glEnableVertexAttribArray(i);
			} else if (elem == VertexFormatElement.COLOR) {
				glVertexAttribPointer(i, 4, GL_FLOAT, true, 48, 12);
				glEnableVertexAttribArray(i);
			} else if (elem == VertexFormatElement.NORMAL) {
				glVertexAttribPointer(i, 3, GL_BYTE, true, 48, 36);
				glEnableVertexAttribArray(i);
			} else if (elem == VertexFormatElement.UV1) {
				glVertexAttribIPointer(i, 2, GL_UNSIGNED_SHORT, 48, 40);
				glEnableVertexAttribArray(i);
			} else if (elem == VertexFormatElement.UV2) {
				glVertexAttribIPointer(i, 2, GL_UNSIGNED_SHORT, 48, 44);
				glEnableVertexAttribArray(i);
			}
		}
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

	@Override
	public void applyComputeShader(PoseStack poseStack, float r, float g, float b, float a, int overlay, int light, int jointCount) {
		ComputeProgram shader = ComputeShaderProvider.meshComputeVanilla;
		shader.useProgram();
		shader.getUniform("colorIn").uploadVec4(r, g, b, a);
		shader.getUniform("uv1In").uploadUnsignedInt(overlay);
		shader.getUniform("uv2In").uploadUnsignedInt(light);
		shader.getUniform("part_offset").uploadUnsignedInt(jointCount);
		shader.getUniform("model_view_matrix").uploadMatrix4f(poseStack.last().pose());
		shader.getUniform("normal_matrix").uploadMatrix3f(poseStack.last().normal());
		if (this.usePersist) {
			this.poseBuffer.bindRange(GL43C.GL_SHADER_STORAGE_BUFFER, 0, this.posesOff, this.poseSize);
            this.hfBuffer.bindRange(GL43C.GL_SHADER_STORAGE_BUFFER, 4, this.hiddenFlagOff, this.hiddenFlags.length * 4L);
		} else {
			curr_POSE_BO.bindBufferBase(0);
			curr_hiddenFlagsBO.bindBufferBase(4);
		}

		this.elementsBO.bindBufferBase(1);
		this.vObjBO.bindBufferBase(2);
		this.jointBO.bindBufferBase(3);
		this.outVertexAttrBO.bindBufferBase(5);

		int workGroupCount = (this.vcount + WORK_GROUP_SIZE - 1) / WORK_GROUP_SIZE;
		if (this.usePersist) GL46C.glMemoryBarrier(GL46C.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
		shader.dispatch(workGroupCount, 1, 1);
		shader.memBarriers();

		if (this.usePersist) {
			GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, 0);
			GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 4, 0);
		} else {
			curr_POSE_BO.unbind();
			curr_hiddenFlagsBO.unbind();
		}

		this.elementsBO.unbind();
		this.vObjBO.unbind();
		this.jointBO.unbind();
		this.outVertexAttrBO.unbind();
	}

	private long hiddenFlagOff = 0;
	private long posesOff = 0;
	private long poseSize = 0;
	private boolean usePersist = true;
	private MappedBuffer poseBuffer;
	private MappedBuffer hfBuffer;

	@Override
	public void drawWithShader(SkinnedMesh skinnedMesh, PoseStack poseStack, MultiBufferSource buffers, RenderType renderType, int packedLight, float r, float g, float b, float a, int overlay, @Nullable Armature armature, OpenMatrix4f[] poses) {
		// pose setup and upload
		this.usePersist = ClientConfig.activatePersistentBuffer && ComputeShaderProvider.supportPersistentMapping();
		for (int i = 0; i < poses.length; i++) {
			TOTAL_POSES[i].load(poses[i]);

			if (armature != null) {
				TOTAL_POSES[i].mulBack(armature.searchJointById(i).getToOrigin());
			}
		}

		if(usePersist)
			Arrays.fill(this.hiddenFlags, 0);
		else
			Arrays.fill(HF, 0);

		for (SkinnedMeshPart part : skinnedMesh.getAllParts()) {
			OpenMatrix4f mat = part.getVanillaPartTransform();
			if (mat == null) mat = OpenMatrix4f.IDENTITY;
			TOTAL_POSES[poses.length + part.getPartVBO().partIdx()].load(mat);

			if (!part.isHidden()) continue;

			int flagPos = part.getPartVBO().partIdx() / 32;
			int flagOffset = part.getPartVBO().partIdx() % 32;
			if(usePersist){
				int flag = this.hiddenFlags[flagPos];
				this.hiddenFlags[flagPos] = flag | ((part.isHidden() ? 1:0) << flagOffset);
			}
			else {
				int flag = HF[flagPos];
				HF[flagPos] = flag | ((part.isHidden() ? 1:0) << flagOffset);
			}
		}
		if (this.usePersist) {
            // pose
			int poseLen = poses.length + skinnedMesh.getAllParts().size();
            this.poseSize = ComputeShaderProvider.align(poseLen * 16 * 4L, ComputeShaderProvider.getSSBOAlignment());
            this.poseBuffer = ComputeShaderProvider.posesBufferPool.getOrWait(this.poseSize);
			long addressBase = this.poseBuffer.reserve(this.poseSize, true);
			var tmp = this.poseBuffer.addressAt(0);
            this.posesOff = addressBase - tmp;

			// upload
			for (int i = 0; i < poseLen; i++) {
				TOTAL_POSES[i].store(addressBase + (16L * 4 * i));
			}

			// hidden flag
            this.hfBuffer = ComputeShaderProvider.hiddenFlagPool.getOrWait(this.hiddenFlags.length * 4L);
			var aligned_size = ComputeShaderProvider.align(this.hiddenFlags.length * 4L, ComputeShaderProvider.getSSBOAlignment());

			addressBase = this.hfBuffer.reserve(aligned_size);
            this.hiddenFlagOff = addressBase - this.hfBuffer.addressAt(0);

			for (int i = 0; i < this.hiddenFlags.length; i++) {
				int hf = this.hiddenFlags[i];
				MemoryUtil.memPutInt(addressBase + 4L * i, hf);
			}
		} else {
			curr_POSE_BO = POSE_BO_POOL.getOrWait();
			curr_hiddenFlagsBO = HF_BO_POOL.getOrWait();
			curr_POSE_BO.updateFromTo(0, poses.length + skinnedMesh.getAllParts().size());
			curr_hiddenFlagsBO.updateFromTo(0, (skinnedMesh.getAllParts().size() + 31) / 32);
		}
		//if(!usePersist) GL33C.glFinish();
		// state trace
		int currentBoundVao = GlStateManager._getInteger(GLConstants.GL_VERTEX_ARRAY_BINDING);
		int currentBoundVbo = GlStateManager._getInteger(GLConstants.GL_VERTEX_ARRAY_BUFFER_BINDING);

		// setup state
		GlStateManager._glBindVertexArray(this.arrayObjectId);

		this.draw(poseStack, renderType, r, g, b, a, overlay, packedLight, poses.length);

		if (buffers instanceof OutlineBufferSource outlineBufferSource) {
			renderType.outline().ifPresent(outlineRendertype -> {
				this.draw(poseStack, outlineRendertype, ((yesman.epicfight.mixin.client.OutlineBufferSourceAccessor) outlineBufferSource).epicfight$getTeamR() / 255.0F,
						((yesman.epicfight.mixin.client.OutlineBufferSourceAccessor) outlineBufferSource).epicfight$getTeamG() / 255.0F, ((yesman.epicfight.mixin.client.OutlineBufferSourceAccessor) outlineBufferSource).epicfight$getTeamB() / 255.0F,
						((yesman.epicfight.mixin.client.OutlineBufferSourceAccessor) outlineBufferSource).epicfight$getTeamA() / 255.0F, overlay, packedLight, poses.length);
			});
		}
		GlStateManager._glBindVertexArray(currentBoundVao);
		GlStateManager._glBindBuffer(GLConstants.GL_ARRAY_BUFFER, currentBoundVbo);
	}
	
	@Override
	public int vaoId() {
		return this.arrayObjectId;
	}
	
	@Override
	public int vertexCount() {
		return this.vcount;
	}
}

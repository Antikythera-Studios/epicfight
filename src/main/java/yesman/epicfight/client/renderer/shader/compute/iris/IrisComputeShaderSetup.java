package yesman.epicfight.client.renderer.shader.compute.iris;

import static org.lwjgl.opengl.GL11C.GL_BYTE;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_SHORT;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.glVertexAttribIPointer;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.irisshaders.iris.layer.BufferSourceWrapper;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryUtil;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.SkinnedMesh.SkinnedMeshPart;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.GLConstants;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.shader.compute.ComputeShaderSetup;
import yesman.epicfight.client.renderer.shader.compute.backend.buffers.MappedBuffer;
import yesman.epicfight.client.renderer.shader.compute.backend.ssbo.StaticSSBO;
import yesman.epicfight.client.renderer.shader.compute.backend.program.ComputeProgram;
import yesman.epicfight.client.renderer.shader.compute.loader.ComputeShaderProvider;
import yesman.epicfight.config.ClientConfig;

public class IrisComputeShaderSetup extends ComputeShaderSetup {
	protected StaticSSBO<Float> midUVBO;
	
	public IrisComputeShaderSetup(SkinnedMesh skinnedMesh) {
		super(skinnedMesh, 15);
	}
	
	@Override
	protected void initAttachmentSSBO(List<ElemInfo> elements, List<Float> uvList) {
		List<Float> midUVList = Lists.newArrayList();
		float[] midUVs = new float[(elements.size() / 3) * 2];
		
		for (int i = 0; i < elements.size(); i++) {
			int vertPoolIdx = elements.get(i).poolId();
			float u = uvList.get(vertPoolIdx * 2);
			float v = uvList.get(vertPoolIdx * 2 + 1);
			int faceIdx = i / 3;

			if (i % 3 == 0) {
				midUVs[faceIdx * 2] = u / 3;
				midUVs[faceIdx * 2 + 1] = v / 3;
			} else {
				midUVs[faceIdx * 2] += u / 3;
				midUVs[faceIdx * 2 + 1] += v / 3;
			}
		}
		
		for (int i = 0; i < elements.size(); i++) {
			int faceIdx = i / 3;
			midUVList.add(midUVs[faceIdx * 2]);
			midUVList.add(midUVs[faceIdx * 2 + 1]);
		}
		
		this.midUVBO = new StaticSSBO<> (midUVList, 1, (v, b) -> b.put(v));
	}
	
	@Override
	public void bindBufferFormat(VertexFormat vertexFormat) {
		var elems = vertexFormat.getElements();
		
		glBindBuffer(GL_ARRAY_BUFFER, outVertexAttrBO.glSSBO);
		
		int midUvPos = -1;
		
		for (int i = 0; i < elems.size(); ++i) {
			VertexFormatElement elem = elems.get(i);
			
			if (elem == VertexFormatElement.POSITION) {
				glVertexAttribPointer(i, 3, GL_FLOAT, false, 60, 0);
				glEnableVertexAttribArray(i);
			} else if (elem == VertexFormatElement.UV0) {
				glVertexAttribPointer(i, 2, GL_FLOAT, false, 60, 28);
				glEnableVertexAttribArray(i);
			} else if (elem == VertexFormatElement.COLOR) {
				glVertexAttribPointer(i, 4, GL_FLOAT, true, 60, 12);
				glEnableVertexAttribArray(i);
			} else if (elem == VertexFormatElement.NORMAL) {
				glVertexAttribPointer(i, 3, GL_BYTE, true, 60, 36);
				glEnableVertexAttribArray(i);
			} else if (elem == VertexFormatElement.UV1) {
				glVertexAttribIPointer(i, 2, GL_UNSIGNED_SHORT, 60, 40);
				glEnableVertexAttribArray(i);
			} else if (elem == VertexFormatElement.UV2) {
				glVertexAttribIPointer(i, 2, GL_UNSIGNED_SHORT, 60, 44);
				glEnableVertexAttribArray(i);
			}
			// iris part
			else if (elem == IrisVertexFormats.ENTITY_ID_ELEMENT) {
				glVertexAttribIPointer(i, 3, GL_UNSIGNED_SHORT, 60, 48);
				glEnableVertexAttribArray(i);
			} else if (elem == IrisVertexFormats.MID_TEXTURE_ELEMENT) {
				midUvPos = i;
			} else if (elem == IrisVertexFormats.TANGENT_ELEMENT) {
				glVertexAttribPointer(i, 4, GL_BYTE, false, 60, 56);
				glEnableVertexAttribArray(i);
			}
		}
		
		if (midUvPos >= 0) {
			glBindBuffer(GL_ARRAY_BUFFER, midUVBO.glSSBO);
			glVertexAttribPointer(midUvPos, 2, GL_FLOAT, false, 0, 0);
			glEnableVertexAttribArray(midUvPos);
		}
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

	@Override
	public void applyComputeShader(PoseStack poseStack, float r, float g, float b, float a, int overlay, int light, int jointCount) {
		// shader setup
		ComputeProgram shader = ComputeShaderProvider.meshComputeIris;
		shader.useProgram();
		shader.getUniform("colorIn").uploadVec4(r, g, b, a);
		shader.getUniform("uv1In").uploadUnsignedInt(overlay);
		shader.getUniform("uv2In").uploadUnsignedInt(light);
		shader.getUniform("part_offset").uploadUnsignedInt(jointCount);
		shader.getUniform("entity_id_0").uploadUnsignedInt(((getEntity() << 16) & 0xFFFF0000) | (getBlock() & 0xFFFF));
		shader.getUniform("entity_id_1").uploadUnsignedInt(getItem() << 16);
		shader.getUniform("model_view_matrix").uploadMatrix4f(poseStack.last().pose());
		shader.getUniform("normal_matrix").uploadMatrix3f(poseStack.last().normal());
		if(use_persist){
			pose_buffer.bindRange(GL43C.GL_SHADER_STORAGE_BUFFER, 0,
					poses_off, pose_size);
			hf_buffer.bindRange(GL43C.GL_SHADER_STORAGE_BUFFER, 4,
					hidden_flag_off, HF.length * 4L);
		}
		else {
			//ComputeShaderSetup.POSE_BO.bindBufferBase(0);
			curr_POSE_BO.bindBufferBase(0);
			curr_hiddenFlagsBO.bindBufferBase(4);
		}

		this.elementsBO.bindBufferBase(1);
		this.vObjBO.bindBufferBase(2);
		this.jointBO.bindBufferBase(3);
		this.outVertexAttrBO.bindBufferBase(5);
		
		int workGroupCount = ((this.vcount / 3) + WORK_GROUP_SIZE - 1) / WORK_GROUP_SIZE;
		if (use_persist) GL46C.glMemoryBarrier(GL46C.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
		shader.dispatch(workGroupCount, 1, 1);
		shader.memBarriers();

		if(use_persist){
			GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, 0);
			GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 4, 0);
		}
		else {
			curr_POSE_BO.unbind();
			curr_hiddenFlagsBO.unbind();
		}

		this.elementsBO.unbind();
		this.vObjBO.unbind();
		this.jointBO.unbind();
		this.outVertexAttrBO.unbind();
	}

	private long hidden_flag_off = 0;
	private long poses_off = 0;
	private long pose_size = 0;
	private boolean use_persist = true;
	private MappedBuffer pose_buffer;
	private MappedBuffer hf_buffer;

	@Override
	public void drawWithShader(SkinnedMesh skinnedMesh, PoseStack poseStack, MultiBufferSource buffers, RenderType renderType, int packedLight, float r, float g, float b, float a, int overlay, @Nullable Armature armature, OpenMatrix4f[] poses) {
		// pose setup and upload
		use_persist = ClientConfig.activatePersistentBuffer && ComputeShaderProvider.supportPersistentMapping();
		for (int i = 0; i < poses.length; i++) {
			TOTAL_POSES[i].load(poses[i]);
			
			if (armature != null) {
				TOTAL_POSES[i].mulBack(armature.searchJointById(i).getToOrigin());
			}
		}

		if(use_persist)
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
			if(use_persist){
				int flag = this.hiddenFlags[flagPos];
				this.hiddenFlags[flagPos] = flag | ((part.isHidden() ? 1:0) << flagOffset);
			}
			else {
				int flag = HF[flagPos];
				HF[flagPos] = flag | ((part.isHidden() ? 1:0) << flagOffset);
			}
		}

		if(use_persist){
			// pose
			int pose_len = poses.length + skinnedMesh.getAllParts().size();
			pose_size = ComputeShaderProvider.align(pose_len * 16 * 4L,
					ComputeShaderProvider.getSSBOAlignment());
			pose_buffer = ComputeShaderProvider.posesBufferPool.getOrWait(
					pose_size
			);

			long address_base = pose_buffer.reserve(pose_size, true);
			var tmp = pose_buffer.addressAt(0);
			poses_off = address_base - tmp;

			// upload

			for (int i = 0; i < pose_len; i++) {
				TOTAL_POSES[i].store(address_base + (16L * 4 * i));
			}

			// hidden flag
			hf_buffer = ComputeShaderProvider.hiddenFlagPool.getOrWait(
					hiddenFlags.length * 4L
			);

			var aligned_size = ComputeShaderProvider.align(hiddenFlags.length * 4L,
					ComputeShaderProvider.getSSBOAlignment());

			address_base = hf_buffer.reserve(aligned_size);
			hidden_flag_off = address_base - hf_buffer.addressAt(0);

			for (int i = 0; i < hiddenFlags.length; i++) {
				int hf = hiddenFlags[i];
				MemoryUtil.memPutInt(address_base + 4L * i, hf);
			}
		}
		else {
			curr_POSE_BO = POSE_BO_POOL.getOrWait();
			curr_hiddenFlagsBO = HF_BO_POOL.getOrWait();
			curr_POSE_BO.updateFromTo(0, poses.length + skinnedMesh.getAllParts().size());
			curr_hiddenFlagsBO.updateFromTo(0, (skinnedMesh.getAllParts().size() + 31) / 32);
		}
		
		// state trace
		int currentBoundVao = GlStateManager._getInteger(GLConstants.GL_VERTEX_ARRAY_BINDING);
		int currentBoundVbo = GlStateManager._getInteger(GLConstants.GL_VERTEX_ARRAY_BUFFER_BINDING);
		
		// setup state
		GlStateManager._glBindVertexArray(this.arrayObjectId);
		this.draw(poseStack, renderType, r, g, b, a, overlay, packedLight, poses.length);
		
		if (buffers instanceof BufferSourceWrapper bufferwrapper) {
			if (bufferwrapper.getOriginal() instanceof OutlineBufferSource outlineBufferSource) {
				renderType.outline().ifPresent(outlineRendertype -> {
					this.draw(poseStack, outlineRendertype, ((yesman.epicfight.mixin.client.OutlineBufferSourceAccessor) outlineBufferSource).epicfight$getTeamR() / 255.0F,
							((yesman.epicfight.mixin.client.OutlineBufferSourceAccessor) outlineBufferSource).epicfight$getTeamG() / 255.0F, ((yesman.epicfight.mixin.client.OutlineBufferSourceAccessor) outlineBufferSource).epicfight$getTeamB() / 255.0F,
							((yesman.epicfight.mixin.client.OutlineBufferSourceAccessor) outlineBufferSource).epicfight$getTeamA() / 255.0F, overlay, packedLight, poses.length);
				});
			}
		} else if (buffers instanceof OutlineBufferSource outlineBufferSource) {
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
	public void destroyBuffers() {
		this.midUVBO.close();
		super.destroyBuffers();
	}

	@Override
	public int vertexCount() {
		return this.vcount;
	}
	
    static short getBlock() {
        return (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
    }

	static short getEntity() {
        return (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
    }

	static short getItem() {
        return (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
    }
}

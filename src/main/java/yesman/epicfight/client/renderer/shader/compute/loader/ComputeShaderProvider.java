package yesman.epicfight.client.renderer.shader.compute.loader;


import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.client.renderer.shader.compute.ComputeShaderSetup;
import yesman.epicfight.client.renderer.shader.compute.VanillaComputeShaderSetup;
import yesman.epicfight.client.renderer.shader.compute.backend.pool.BuffersPool;
import yesman.epicfight.client.renderer.shader.compute.backend.program.BarrierFlags;
import yesman.epicfight.client.renderer.shader.compute.backend.program.ComputeProgram;
import yesman.epicfight.client.renderer.shader.compute.backend.ssbo.DynamicSSBO;
import yesman.epicfight.client.renderer.shader.compute.backend.ssbo.IArrayBufferProxy;
import yesman.epicfight.client.renderer.shader.compute.iris.IrisComputeShaderSetup;

import java.nio.FloatBuffer;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ComputeShaderProvider {
    public static ComputeProgram meshComputeVanilla;
    public static ComputeProgram batchedMeshComputeVanilla;
    public static ComputeProgram meshComputeIris;

    public static BuffersPool posesBufferPool;
    public static BuffersPool hiddenFlagPool;

    private static boolean supportComputeShader = false;
    private static boolean supportPersistentMapping = false;

    private static boolean irisLoaded = false;
    private static long maxSSBOLen = 0;
    private static long ssboAlignment = 0;

    private static Function<SkinnedMesh, ComputeShaderSetup> computeShaderProvider = VanillaComputeShaderSetup::new;
    
    public static void initIris() {
    	irisLoaded = true;
    	computeShaderProvider = IrisComputeShaderSetup::new;
    }
    
    public static boolean supportComputeShader() {
    	return supportComputeShader;
    }

    public static boolean supportPersistentMapping() {
        return supportPersistentMapping;
    }

    public static boolean irisLoaded() {
        return irisLoaded;
    }
    
    public static void checkIfSupports() {
    	String glVersion = GL33C.glGetString(GL33C.GL_VERSION);
        int major = GL33C.glGetInteger(GL33C.GL_MAJOR_VERSION);
        int minor = GL33C.glGetInteger(GL33C.GL_MINOR_VERSION);
        GLCapabilities capabilities = GL.getCapabilities();

        supportComputeShader = ((major > 4) || (major == 4 && minor >= 3))
                || (capabilities.GL_ARB_compute_shader && capabilities.GL_ARB_shader_storage_buffer_object);
        supportPersistentMapping = ((major > 4) || (major == 4 && minor >= 6))
                || capabilities.GL_ARB_buffer_storage;

        EpicFight.LOGGER.warn("[Computer Shader Acceleration] OpenGL Version: {}", glVersion);
        EpicFight.LOGGER.warn("[Computer Shader Acceleration] Compute Shader: {}", (supportComputeShader ? "Supported" : "Unsupported"));
    }
    
    public static void epicfight$registerComputeShaders(yesman.epicfight.platform.neoforged.client.event.RegisterShadersEvent event) {
        if (!supportComputeShader) return;
        
        clear();
        
        try {
            meshComputeVanilla = ComputeShaderLoader.loadComputeShaderProgram(event.getResourceProvider(),
                    EpicFight.identifier("shaders/compute/vanilla_mesh_transformer.comp"),
                    BarrierFlags.SHADER_STORAGE, BarrierFlags.VERTEX_ATTRIB_ARRAY);
            /*batchedMeshComputeVanilla =
                    ComputeShaderLoader.loadComputeShaderProgram(event.getResourceProvider(),
                            EpicFight.identifier("shaders/compute/batched/vanilla_batched_mesh_transformer.comp"),
                            BarrierFlags.SHADER_STORAGE, BarrierFlags.VERTEX_ATTRIB_ARRAY);*/
            if (irisLoaded)
            {
                meshComputeIris = ComputeShaderLoader.loadComputeShaderProgram(event.getResourceProvider(), EpicFight.identifier("shaders/compute/iris_mesh_transformer.comp"), BarrierFlags.SHADER_STORAGE, BarrierFlags.VERTEX_ATTRIB_ARRAY);
            }

            maxSSBOLen = ComputeShaderLoader.getGLMaxSSBOSize();
            ssboAlignment = ComputeShaderLoader.getSSBOAlignment();

            if(supportPersistentMapping) {
                posesBufferPool = new BuffersPool(8, 256 * 1024 * 16 * 4);
                hiddenFlagPool = new BuffersPool(8,  16 * 1024 * 4);
            }

        } catch (Exception e) {
            supportComputeShader = false;
            EpicFight.LOGGER.warn("[Computer Shader Acceleration] There were some errors while loading the compute shader, and this feature will be forcibly disabled.");
            EpicFight.LOGGER.warn("[Computer Shader Acceleration] Detail: {0}", e);
        }
    }

    public static long getMaxSSBO(){
        return maxSSBOLen;
    }

    public static long getSSBOAlignment(){
        return ssboAlignment;
    }

    public static long align(long offset, long align) {
        return (offset + align - 1) / align * align;
    }

    public static void clear() {
        if (meshComputeVanilla != null) {
        	meshComputeVanilla.delete();
        }
        
        if (meshComputeIris != null) {
        	meshComputeIris.delete();
        }
    }
    
    public static ComputeShaderSetup getComputeShaderSetup(SkinnedMesh mesh) {
    	return computeShaderProvider.apply(mesh);
    }
    
    public static <T> IArrayBufferProxy createDynamicBuffer(T[] src, int srcSize, BiConsumer<T, FloatBuffer> uploader){
        return new DynamicSSBO<>(src, (short) srcSize, DynamicSSBO.DataMode.DYNAMIC, uploader);
    }
}

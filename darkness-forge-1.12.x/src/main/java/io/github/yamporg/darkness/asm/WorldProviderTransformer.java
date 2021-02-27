package io.github.yamporg.darkness.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public final class WorldProviderTransformer implements IClassTransformer {
    private static final String WORLD_PROVIDER = "net.minecraft.world.WorldProvider";
    private static final String WORLD_PROVIDER_END = "net.minecraft.world.WorldProviderEnd";
    private static final String WORLD_PROVIDER_HELL = "net.minecraft.world.WorldProviderHell";

    private static final String GET_FOG_COLOR_OWNER = WORLD_PROVIDER.replace('.', '/');
    private static final String GET_FOG_COLOR_DESC = "(FF)Lnet/minecraft/util/math/Vec3d;";
    private static final String GET_FOG_COLOR_NAME =
            FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(
                    GET_FOG_COLOR_OWNER, "func_76562_b", GET_FOG_COLOR_DESC);

    private static final String WORLD_PROVIDER_HOOKS =
            WorldProviderHooks.class.getName().replace('.', '/');
    private static final String ON_GET_FOG_COLOR_DESC =
            "(Lnet/minecraft/world/WorldProvider;FF)Lnet/minecraft/util/math/Vec3d;";
    private static final String ON_GET_FOG_COLOR_NAME = "onGetFogColor";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!WORLD_PROVIDER_END.equals(transformedName)
                && !WORLD_PROVIDER_HELL.equals(transformedName)) {
            return basicClass;
        }

        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cr.accept(
                new ClassVisitor(Opcodes.ASM5, cw) {
                    @Override
                    public MethodVisitor visitMethod(
                            int access,
                            String name,
                            String desc,
                            String signature,
                            String[] exceptions) {
                        MethodVisitor mv =
                                super.visitMethod(access, name, desc, signature, exceptions);
                        if (!GET_FOG_COLOR_NAME.equals(name) || !GET_FOG_COLOR_DESC.equals(desc)) {
                            return mv;
                        }
                        return new GeneratorAdapter(api, mv, access, name, desc) {
                            @Override
                            public void visitCode() {
                                super.visitCode();
                                loadThis();
                                loadArgs();
                                invokeStatic(
                                        Type.getObjectType(WORLD_PROVIDER_HOOKS),
                                        new Method(ON_GET_FOG_COLOR_NAME, ON_GET_FOG_COLOR_DESC));
                                // Generate bytecode that returns result iff it’s not null.
                                // We have to dup the value on the stack because ifnull pops it.
                                Label label = newLabel();
                                dup();
                                ifNull(label);
                                returnValue();
                                mark(label);
                                pop();
                            }
                        };
                    }
                },
                ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }
}

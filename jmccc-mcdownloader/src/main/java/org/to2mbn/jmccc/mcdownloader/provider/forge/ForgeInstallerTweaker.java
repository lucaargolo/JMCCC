package org.to2mbn.jmccc.mcdownloader.provider.forge;

import org.objectweb.asm.*;

import java.io.InputStream;

public class ForgeInstallerTweaker {
    //https://github.com/MinecraftForge/Installer/blob/2.0/src/main/java/net/minecraftforge/installer/SimpleInstaller.java
    public static byte[] tweakSimpleInstaller(InputStream is, boolean bl) throws Exception {
        ClassReader cr = new ClassReader(is);
        ClassWriter cw = new ClassWriter(0);
        SimpleInstallerClassVisitor cv = new SimpleInstallerClassVisitor(Opcodes.ASM9, cw, bl);
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    private static class SimpleInstallerClassVisitor extends ClassVisitor {
        private final boolean bl;

        protected SimpleInstallerClassVisitor(int api, ClassVisitor classVisitor, boolean bl) {
            super(api, classVisitor);
            this.bl = bl;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (name.equals("main")) {
                return new MainMethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions), this.bl);
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    private static class MainMethodVisitor extends MethodVisitor {
        private final boolean bl;

        protected MainMethodVisitor(int api, MethodVisitor methodVisitor, boolean bl) {
            super(api, methodVisitor);
            this.bl = bl;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            // Change Actions.SERVER -> Actions.CLIENT
            if (bl && opcode == Opcodes.GETSTATIC && name.equals("SERVER")) {
                name = "CLIENT";
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            // Remove System.exit
            if (opcode == Opcodes.INVOKESTATIC && name.equals("exit")) {
                super.visitInsn(Opcodes.POP);
                return;
            }
            // Remove launchGui
            if(opcode == Opcodes.INVOKESPECIAL && name.equals("launchGui")) {
                super.visitInsn(Opcodes.POP);
                super.visitInsn(Opcodes.RETURN);
                return;
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}

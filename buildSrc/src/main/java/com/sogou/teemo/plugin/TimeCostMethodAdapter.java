package com.sogou.teemo.plugin;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class TimeCostMethodAdapter extends AdviceAdapter {
    private final String methodName;
    private final String className;
    protected TimeCostMethodAdapter(int api, MethodVisitor mv, int access, String name, String desc, String className) {
        super(api, mv, access, name, desc);
        this.className = className;
        this.methodName = name;
    }
    private int timeLocalIndex = 0;
    @Override
    protected void onMethodEnter() {
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        timeLocalIndex = newLocal(Type.LONG_TYPE); //这个是LocalVariablesSorter 提供的功能，可以尽量复用以前的局部变量
        mv.visitVarInsn(LSTORE, timeLocalIndex);
    }

    @Override
    protected void onMethodExit(int opcode) {
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        mv.visitVarInsn(LLOAD, timeLocalIndex);
        mv.visitInsn(LSUB);//此处的值在栈顶
        mv.visitVarInsn(LSTORE, timeLocalIndex);//因为后面要用到这个值所以先将其保存到本地变量表中


        int stringBuilderIndex = newLocal(Type.getType("Ljava/lang/StringBuilder"));
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ASTORE, stringBuilderIndex);//需要将栈顶的 stringbuilder 保存起来否则后面找不到了
        mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex);
        mv.visitLdcInsn(className + "." + methodName + " time:");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitInsn(Opcodes.POP);
        mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex);
        mv.visitVarInsn(Opcodes.LLOAD, timeLocalIndex);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
        mv.visitInsn(Opcodes.POP);
        mv.visitLdcInsn("Geek");
        mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false);//注意： Log.d 方法是有返回值的，需要 pop 出去
        mv.visitInsn(Opcodes.POP);//插入字节码后要保证栈的清洁，不影响原来的逻辑，否则就会产生异常，也会对其他框架处理字节码造成影响
    }

    @Override
    public void visitMethodInsn(int opcodeAndSource, String owner, String name, String descriptor, boolean isInterface) {
        if (owner.equals("android/telephony/TelephonyManager") && name.equals("getDeviceId") && descriptor.equals("()Ljava/lang/String;")) {
            System.out.println(String.format("getDeviceId invoke by className:%s, methodName:%s", className, methodName));
        }
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
    }
}

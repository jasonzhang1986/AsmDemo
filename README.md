# AsmDemo
asm 操作字节码的 demo，统计各个方法的耗时

插件的 module 名字改为 buildSrc，这样修改代码后可以直接在 app module 中 build 生效

入口是 TimeCostPlugin
```java
AppExtension appExtension = project.getExtensions().getByType(AppExtension.class);
appExtension.registerTransform(new TimeCostTransform());
```
注册自定义的 Transform，自定义的 Transform 的核心方法是
```java
public void transform(TransformInvocation transformInvocation) 
```
这里分 jar 和目录两个方式遍历input，jar其实是一个压缩包通过 ZipOutStream 的方式对 class 文件修改后重新写入到 jar，目录遍历最后判断是 class 文件也是通过 ASM 对 class 文件修改后重新写入
```java
//通过asm修改源class文件
ClassReader classReader = new ClassReader(inputStream);
ClassWriter classWriter = new ClassWriter(0);
TimeCostClassVisitor timeCostClassVisitor = new TimeCostClassVisitor(classWriter);
classReader.accept(timeCostClassVisitor, ClassReader.EXPAND_FRAMES);
byte[] data = classWriter.toByteArray();
```
ASM为我们提供了类的访问接口
```java
public class TimeCostClassVisitor extends ClassVisitor implements Opcodes {

    private String mPackage;//包名
    private String mCurClassName;//当前访问的类的全限定名

    public TimeCostClassVisitor(ClassVisitor classVisitor) {
        super(ASM7, classVisitor);
        mPackage = TimeCostPlugin.sPackage;
        if(mPackage.length() > 0){
            mPackage = mPackage.replace(".", "/");
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        mCurClassName = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
        //修改方法
        return new TimeCostMethodAdapter(api, methodVisitor,access, name, desc, this.mCurClassName);
    }
}
```
通过 visitMethod 来访问和修改方法，ASM 为我们提供了AdviceAdapter的类，方便在方法的入口和出口处添加代码
```java
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
}

```

另外也可以通过继承MethodVisitor来修改 method
```java
class TimeCostMethodVisitor extends LocalVariablesSorter implements Opcodes {

    //局部变量
    int startTime, endTime, costTime, thisMethodStack;

    public TimeCostMethodVisitor(MethodVisitor methodVisitor, int access, String desc) {
        super(ASM7, access, desc, methodVisitor);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        //long startTime = System.currentTimeMillis();
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        startTime = newLocal(Type.LONG_TYPE);
        mv.visitVarInsn(LSTORE, startTime);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == RETURN || opcode == IRETURN) {
            //long endTime = System.currentTimeMillis();
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            endTime = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(LSTORE, endTime);

            //long costTime = endTime - startTime;
            mv.visitVarInsn(LLOAD, endTime);
            mv.visitVarInsn(LLOAD, startTime);
            mv.visitInsn(LSUB);
            costTime = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(LSTORE, costTime);

            //判断costTime是否大于sThreshold
            mv.visitVarInsn(LLOAD, costTime);
            mv.visitLdcInsn(new Long(TimeCostPlugin.sThreshold));
            mv.visitInsn(LCMP);

            //if costTime <= sThreshold,就跳到end标记处，否则继续往下执行
            Label end = new Label();
            mv.visitJumpInsn(IFLE, end);

            //StackTraceElement thisMethodStack = (new Exception()).getStackTrace()[0]
            mv.visitTypeInsn(NEW, "java/lang/Exception");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Exception", "<init>", "()V", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false);
            mv.visitInsn(ICONST_0);
            mv.visitInsn(AALOAD);
            thisMethodStack = newLocal(Type.getType(StackTraceElement.class));
            mv.visitVarInsn(ASTORE, thisMethodStack);

            //Log.e("rain", String.format（"===> %s.%s(%s:%s)方法耗时 %d ms", thisMethodStack.getClassName(), thisMethodStack.getMethodName(),thisMethodStack.getFileName(),thisMethodStack.getLineNumber(),costTime));
            mv.visitLdcInsn("TimeCost");
            mv.visitLdcInsn("===> %s.%s(%s:%s)\u65b9\u6cd5\u8017\u65f6 %d ms");
            mv.visitInsn(ICONST_5);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ALOAD, thisMethodStack);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;", false);
            mv.visitInsn(AASTORE);
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_1);
            mv.visitVarInsn(ALOAD, thisMethodStack);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getMethodName", "()Ljava/lang/String;", false);
            mv.visitInsn(AASTORE);
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_2);
            mv.visitVarInsn(ALOAD, thisMethodStack);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getFileName", "()Ljava/lang/String;", false);
            mv.visitInsn(AASTORE);
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_3);
            mv.visitVarInsn(ALOAD, thisMethodStack);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getLineNumber", "()I", false);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            mv.visitInsn(AASTORE);
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_4);
            mv.visitVarInsn(LLOAD, costTime);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            mv.visitInsn(AASTORE);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false);
            mv.visitInsn(POP);

            //end标记处，即方法的末尾
            mv.visitLabel(end);
        }
        super.visitInsn(opcode);
    }
}
```

其中字节码的操作代码可以参考 ASM-Online 这个插件生成，该插件需要用 
IntelliJ IDEA 中使用，在 AndroidStudio 中不支持
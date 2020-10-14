package com.sogou.teemo.plugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;


import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class TimeCostTransform extends Transform {

    /**
     * 设置 Transform 的名字
     * @return name
     */
    @Override
    public String getName() {
        return TimeCostTransform.class.getSimpleName();
    }

    /**
     *  APP 可用的 ContentType 有 CLASSES 和 RESOURCES，一般使用CONTENT_CLASS
     * @return
     */
    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    /**
     * APP 可用的 Scope 有 SCOPE_FULL_WITH_FEATURES, SCOPE_FULL_PROJECT, PROJECT_ONLY, SCOPE_FEATURES 等，一般使用SCOPE_FULL_PROJECT
     * @return
     */
    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    /**
     * 是否增量
     * @return
     */
    @Override
    public boolean isIncremental() {
        return true;
    }

    //核心方法
    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        System.out.println("### transform ------------------------ start --------------------------------");
        Collection<TransformInput> transformInputs = transformInvocation.getInputs(); //获取输入
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider(); //获取输出

        //判断 Transform 任务是否是增量的
        boolean isIncremental = transformInvocation.isIncremental();
        System.out.println("### transform , isIncremental = " + isIncremental);

        //如果不是增量，就删除之前的输出，重新开始
        if (!isIncremental) {
            outputProvider.deleteAll();
        }

        for (TransformInput input: transformInputs) {
            Collection<JarInput> jarInputs = input.getJarInputs();
            for (JarInput jarInput: jarInputs) {
                File dest = outputProvider.
                        getContentLocation(jarInput.getFile().getAbsolutePath(),
                                jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                System.out.println("### jarinput:" + jarInput.getName()+", dest="+dest.getAbsolutePath());
                //判断本次Transform任务是否增量
                if(isIncremental){
                    //增量处理Jar文件
                    handleJarIncremental(jarInput, outputProvider);
                }else {
                    //非增量处理Jar文件
                    handleJar(jarInput, outputProvider);
                }
            }

            Collection<DirectoryInput> directoryInputs = input.getDirectoryInputs();
            for (DirectoryInput directoryInput: directoryInputs) {
                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY);
                System.out.println("### directoryInput:"+directoryInput.getName()+", dest="+dest.getAbsolutePath());
                //判断本次Transform任务是否增量
                if(isIncremental){
                    //增量处理目录文件
                    handleDirectoryIncremental(directoryInput, outputProvider);
                }else {
                    //非增量处理目录文件
                    handleDirectory(directoryInput, outputProvider);
                }
            }
        }


        System.out.println("### transform ------------------------  end  --------------------------------");
    }

    private void foreachJarWithTransform(File srcJar, File destJar) throws IOException {
        System.out.println("@@@ foreachJarWithTransform src="+srcJar.getAbsolutePath()+", dest="+destJar.getAbsolutePath());
//        JarFile srcJarFile = new JarFile(srcJar);
//        JarOutputStream destJarFileOs = new JarOutputStream(new FileOutputStream(destJar));
//        Enumeration<JarEntry> enumeration = srcJarFile.entries();
//        //遍历srcJar中的每一条条目
//        while (enumeration.hasMoreElements()) {
//            JarEntry entry = enumeration.nextElement();
//            try (
//                    //获取每一条条目的输入流
//                    InputStream entryIs = srcJarFile.getInputStream(entry)
//            ) {
//                destJarFileOs.putNextEntry(new JarEntry(entry.getName()));
//                if (entry.getName().endsWith(".class")) {//如果是class文件
//                    //通过asm修改源class文件
//                    ClassReader classReader = new ClassReader(entryIs);
//                    ClassWriter classWriter = new ClassWriter(0);
//                    TimeCostClassVisitor timeCostClassVisitor = new TimeCostClassVisitor(classWriter);
//                    classReader.accept(timeCostClassVisitor, ClassReader.EXPAND_FRAMES);
//                    //然后把修改后的class文件复制到destJar中
//                    destJarFileOs.write(classWriter.toByteArray());
//                } else {//如果不是class文件
//                    //原封不动地复制到destJar中
//                    destJarFileOs.write(IOUtils.toByteArray(entryIs));
//                }
//                destJarFileOs.closeEntry();
//            }
//        }
        ZipOutputStream zipOutputStream = null;
        ZipFile zipFile = null;
        File input = srcJar;
        File output = destJar;
        try {
            zipOutputStream = new ZipOutputStream(new FileOutputStream(output));
            zipFile = new ZipFile(input);
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String zipEntryName = zipEntry.getName();

                if (isNeedTraceClass(zipEntryName)) {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
//                    ClassWriter classWriter = ASMCode.run(inputStream);
                    //通过asm修改源class文件
                    ClassReader classReader = new ClassReader(inputStream);
                    ClassWriter classWriter = new ClassWriter(0);
                    TimeCostClassVisitor timeCostClassVisitor = new TimeCostClassVisitor(classWriter);
                    classReader.accept(timeCostClassVisitor, ClassReader.EXPAND_FRAMES);
                    byte[] data = classWriter.toByteArray();
                    InputStream byteArrayInputStream = new ByteArrayInputStream(data);
                    ZipEntry newZipEntry = new ZipEntry(zipEntryName);
                    Util.addZipEntry(zipOutputStream, newZipEntry, byteArrayInputStream);
                } else {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    ZipEntry newZipEntry = new ZipEntry(zipEntryName);
                    Util.addZipEntry(zipOutputStream, newZipEntry, inputStream);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[traceMethodFromJar] err! "+ output.getAbsolutePath());
        } finally {
            try {
                if (zipOutputStream != null) {
                    zipOutputStream.finish();
                    zipOutputStream.flush();
                    zipOutputStream.close();
                }
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public boolean isNeedTraceClass(String fileName) {
        return fileName.endsWith(".class");
    }

    private void handleJarIncremental(JarInput jarInput, TransformOutputProvider outputProvider) throws IOException {
        //获取输入文件的状态
        Status status = jarInput.getStatus();
        //根据文件的Status做出不同的操作
        switch (status){
            case ADDED:
            case CHANGED:
                handleJar(jarInput, outputProvider);
                break;
            case REMOVED:
                //删除所有输出
                outputProvider.deleteAll();
                break;
            case NOTCHANGED:
                //do nothing
                break;
            default:
        }
    }

    private void handleJar(JarInput jarInput, TransformOutputProvider outputProvider) throws IOException {
        //获取输入的jar文件
        File srcJar = jarInput.getFile();
        //使用TransformOutputProvider的getContentLocation方法根据输入构造输出位置
        File destJar = outputProvider.getContentLocation(
                jarInput.getName(),
                jarInput.getContentTypes(),
                jarInput.getScopes(),
                Format.JAR
        );
        //遍历srcJar的所有内容, 在遍历的过程中把srcJar中的内容一条一条地复制到destJar
        //如果发现这个内容条目是class文件，就把它通过asm修改后再复制到destJar中
        foreachJarWithTransform(srcJar, destJar);
    }

    /**
     * 增量处理directory目录中的class文件，可能产生新的输出
     * @throws IOException
     */
    private void handleDirectoryIncremental(DirectoryInput directoryInput, TransformOutputProvider outputProvider) throws IOException {
        //通过DirectoryInput的getChangedFiles方法获取改变过的文件集合，每一个文件对应一个Status
        Map<File, Status> changedFileMap = directoryInput.getChangedFiles();
        //遍历所有改变过的文件
        for (Map.Entry<File, Status> entry : changedFileMap.entrySet()) {
            File file = entry.getKey();
            Status status = entry.getValue();
            File destDirectory = outputProvider.getContentLocation(
                    directoryInput.getName(),
                    directoryInput.getContentTypes(),
                    directoryInput.getScopes(),
                    Format.DIRECTORY
            );
            //根据文件的Status做出不同的操作
            switch (status) {
                case ADDED:
                case CHANGED:
                    transformSingleFile(
                            file,
                            getDestFile(file, directoryInput.getFile(), destDirectory)
                    );
                    break;
                case REMOVED:
                    FileUtils.forceDelete(getDestFile(file, directoryInput.getFile(), destDirectory));
                    break;
                case NOTCHANGED:
                    //do nothing
                    break;
                default:
            }
        }
    }


    /**
     * 处理directory目录中的class文件，产生新的输出
     * @throws IOException
     */
    private void handleDirectory(DirectoryInput directoryInput, TransformOutputProvider outputProvider) throws IOException {
        //获取输入目录代表的File实例
        File srcDirectory = directoryInput.getFile();
        //根据输入构造输出的位置
        File destDirectory = outputProvider.getContentLocation(
                directoryInput.getName(),
                directoryInput.getContentTypes(),
                directoryInput.getScopes(),
                Format.DIRECTORY
        );
        //递归地遍历srcDirectory的所有文件, 在遍历的过程中把srcDirectory中的文件逐个地复制到destDirectory，
        //如果发现这个文件是class文件，就把它通过asm修改后再复制到destDirectory中
        foreachDirectoryRecurseWithTransform(srcDirectory, destDirectory);
    }

    /**
     * (srcDirectory -> destDirectory):
     * 递归地遍历srcDirectory的所有文件, 在遍历的过程中把srcDirectory中的文件逐个地复制到
     * destDirectory，如果发现这个文件是class文件，就把它通过asm修改后再复制到destDirectory中
     * @throws IOException
     */
    private void foreachDirectoryRecurseWithTransform(File srcDirectory, File destDirectory) throws IOException {
        if(!srcDirectory.isDirectory()){
            return;
        }
        File[] files = srcDirectory.listFiles();
        for(File srcFile : files){
            if(srcFile.isFile()){
                File destFile = getDestFile(srcFile, srcDirectory, destDirectory);
                //把srcFile文件复制到destFile中，如果srcFile是class文件，则把它经过asm修改后再复制到destFile中
                transformSingleFile(srcFile, destFile);
            }else {
                //继续递归
                foreachDirectoryRecurseWithTransform(srcFile, destDirectory);
            }
        }
    }

    /**
     * 构造srcFile在destDirectory中对应的destFile
     * @throws IOException
     */
    private File getDestFile(File srcFile, File srcDirectory, File destDirectory) throws IOException {
        String srcDirPath = srcDirectory.getAbsolutePath();
        String destDirPath = destDirectory.getAbsolutePath();
        //找到源输入文件对应的输出文件位置
        String destFilePath = srcFile.getAbsolutePath().replace(srcDirPath, destDirPath);
        //构造源输入文件对应的输出文件
        File destFile = new File(destFilePath);
        FileUtils.touch(destFile);
        return destFile;
    }

    /**
     * (srcFile -> destFile)
     * 把srcFile文件复制到destFile中，如果srcFile是class文件，则把它经过asm修改后再复制到destFile中
     * @throws IOException
     */
    private void transformSingleFile(File srcFile, File destFile) throws IOException {
        try(
                InputStream srcFileIs = new FileInputStream(srcFile);
                OutputStream destFileOs = new FileOutputStream(destFile)
        ){
            if(srcFile.getName().endsWith(".class")){
                ClassReader classReader = new ClassReader(srcFileIs);
                ClassWriter classWriter = new ClassWriter(0);
                TimeCostClassVisitor timeCostClassVisitor = new TimeCostClassVisitor(classWriter);
                classReader.accept(timeCostClassVisitor, ClassReader.EXPAND_FRAMES);
                destFileOs.write(classWriter.toByteArray());
            }else {
                destFileOs.write(IOUtils.toByteArray(srcFileIs));
            }
        }
    }
}

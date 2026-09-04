package com.demo.scanner;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ClassScanner {
    public static List<Class<?>> scan(String packageName){
        List<Class<?>> classes=new ArrayList<>();
        // com.demo.annotation → com/demo/annotation
        String packagePath=packageName.replace(".","/");
        // 获取当前线程的类加载器
        //反射依赖 Class，而 Class 又来自类加载。
        ClassLoader classLoader=Thread.currentThread().getContextClassLoader();
        // 根据路径找到磁盘上对应的文件夹
        URL resource=classLoader.getResource(packagePath);
        if(resource==null){
            throw new RuntimeException("找不到包："+packageName);
        }
        // URL转成File磁盘目录对象
        File directory=new File(resource.getFile());
        // 递归扫描文件夹
        scanDirectory(packageName,directory,classes);
        return classes;
    }
    private static void scanDirectory(String packageName,File directory,List<Class<?>> classes){
        // 获取文件夹下面所有子文件/子文件夹
        File[] files=directory.listFiles();
        if(files==null){
            return;
        }
        for(File file:files){
            // 如果是子文件夹 → 递归继续扫描子包
            if(file.isDirectory()){
                // 拼接子包名 com.demo.annotation.sub
                String subPackage=packageName+"."+file.getName();
                scanDirectory(subPackage,file,classes);
                continue;
            }
            // 只处理 .class 字节码文件，跳过 .java、txt等其他文件
            if(!file.getName().endsWith(".class"))
                continue;
            // 拼装完整全限定类名
            String className=packageName+"."+file.getName().replace(".class","");
            // 反射加载这个类，得到Class对象，存入集合
            try{
                Class<?> clazz=Class.forName(className);
                classes.add((clazz));
            }catch (ClassNotFoundException e){
                throw new RuntimeException("加载类失败："+className,e);
            }
        }

    }
}

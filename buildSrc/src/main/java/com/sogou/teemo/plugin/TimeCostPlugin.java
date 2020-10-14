package com.sogou.teemo.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import com.android.build.gradle.AppExtension;


public class TimeCostPlugin implements Plugin<Project> {
    public static final String sPackage = "com.sogou.teemo.testgc";
    public static final long sThreshold = 3;
    @Override
    public void apply(Project project) {
        project.getTasks().forEach(task -> {
            System.out.println("### task:"+task.getName());
        });
        AppExtension appExtension = project.getExtensions().getByType(AppExtension.class);
        appExtension.registerTransform(new TimeCostTransform());
    }
}

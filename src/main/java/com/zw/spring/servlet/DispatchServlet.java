package com.zw.spring.servlet;

import com.zw.spring.annotation.Autowired;
import com.zw.spring.annotation.Controller;
import com.zw.spring.annotation.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ZhaoWei on 2018/7/1/0001.
 */
public class DispatchServlet extends HttpServlet{

    //类似IOC的容器
    private Map<String, Object> beanMap = new ConcurrentHashMap<String, Object>();

    //
    private Properties contextConfig = new Properties();

    //
    private List<String> classNames = new ArrayList<String>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("---------- 调用doPost ----------");
    }

    /**
     * 初始化过程
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {

        //定位
        doLoadConfig(config.getInitParameter("contextConfig"));

        //加载
        doScanner(contextConfig.getProperty("scanPackage"));

        //注册
        doRegistry();

        //自动依赖注入,在Spring中是通过调用getBean方法才出发依赖注入的
        doAutowired();

        //如果是SpringMVC会多设计一个HnandlerMapping
        //将@RequestMapping中配置的url和一个Method关联上
        //以便于从浏览器获得用户输入的url以后，能够找到具体执行的Method通过反射去调用
        initHandlerMapping();

    }

    private void initHandlerMapping() {
    }

    private void doAutowired() {
        if(beanMap.isEmpty()){
            return;
        }
        for(Map.Entry<String, Object> entry:beanMap.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field:fields){
                //如果不是Autowired注解则跳过
                if(!field.isAnnotationPresent(Autowired.class)){
                    continue;
                }
                Autowired autowired = field.getAnnotation(Autowired.class);
                String beanName = autowired.value().trim();
                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                //强制访问
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(), beanMap.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }



    }

    private void doRegistry() {
        if(classNames.isEmpty()){
            return;
        }
        for(String className : classNames){
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            //Spring中是用多个字方法处理，如：ParseController类似
            if(clazz.isAnnotationPresent(Controller.class)){
                String beanName = lowFirstCase(clazz.getSimpleName());
                try {
                    //Spring此阶段不会直接newInstance放入Map容器，放入的而是BeanDefinition
                    beanMap.put(beanName, clazz.newInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else if(clazz.isAnnotationPresent(Service.class)){
                Service service = clazz.getAnnotation(Service.class);

                //默认用类名首字母小写注入
                //如果自己定义了beanName,则按定义的注入
                //如果是一个接口则使用接口的类型注入

                //Spring中同样会分别调用不同的方法 autowriedByName autowritedByType
                String beanName = service.value();
                if("".equals(beanName.trim())){
                    beanName = lowFirstCase(clazz.getSimpleName());
                }
                try {
                    Object obj = clazz.newInstance();
                    beanMap.put(beanName, obj);

                    Class<?>[] interfaces = clazz.getInterfaces();
                    for(Class i : interfaces){
                        beanMap.put(i.getName(), clazz.newInstance());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.","/"));
        File classDir = new File(url.getFile());
        for(File file : classDir.listFiles()){
            if(file.isDirectory()){
                doScanner(packageName + "." + file.getName());
            }else {
                classNames.add(packageName + "." + file.getName().replace(".class", ""));
            }
        }
    }

    private void doLoadConfig(String c) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(c.replace("classpath:", ""));
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null!=is){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //首字母转为小写
    private String lowFirstCase(String str){
        if(null==str || "".equals(str)){
            return null;
        }
        char[] chars = str.toCharArray();
        chars[0] =+ 32;
        return String.valueOf(chars.toString());
    }

}

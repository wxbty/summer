package ink.zfei.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BeanUtils {
    @SuppressWarnings("unchecked")
    public static void copyProperties(Object target,Object source) throws Exception{
              /*分别获得源对象和目标对象的Class类型对象,Class对象是整个反射机制的源头和灵魂！
                Class对象是在类加载的时候产生,保存着类的相关属性，构造器，方法等信息
              */
        Class sourceClz = source.getClass();
        Class targetClz = target.getClass();
        //得到Class对象所表征的类的所有属性(包括私有属性)
        Field[] fields = sourceClz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fields[i].getName();
            Field targetField  = null;
            try {
                //得到targetClz对象所表征的类的名为fieldName的属性，不存在就进入下次循环
                targetField = targetClz.getDeclaredField(fieldName);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
            //判断sourceClz字段类型和targetClz同名字段类型是否相同
            if(fields[i].getType()==targetField.getType()){
                //由属性名字得到对应get和set方法的名字
                String getMethodName = "get"+fieldName.substring(0,1).toUpperCase()+fieldName.substring(1);
                String setMethodName = "set"+fieldName.substring(0,1).toUpperCase()+fieldName.substring(1);
                //由方法的名字得到get和set方法的Method对象
                Method getMethod;
                try {
                    getMethod = sourceClz.getDeclaredMethod(getMethodName, new Class[]{});
                    Method setMethod = targetClz.getDeclaredMethod(setMethodName, fields[i].getType());
                    //调用source对象的getMethod方法
                    Object result = getMethod.invoke(source, new Object[]{});
                    //调用target对象的setMethod方法
                    setMethod.invoke(target, result);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }else{
                throw new Exception("同名属性类型不匹配！");
            }
        }
    }

}

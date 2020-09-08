package ink.zfei.summer.context;

import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.core.GenericBeanDefinition;
import ink.zfei.summer.xmlParse.Beans;
import ink.zfei.summer.xmlParse.Bean;
import org.apache.commons.beanutils.BeanUtils;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClassPathXmlApplicationContext extends AbstractApplicationContext {

    private String configPath;

    public ClassPathXmlApplicationContext(String configPath) throws IOException {

        super();
        this.configPath = configPath;
        refresh();
    }

    @Override
    protected Map<String, GenericBeanDefinition> loadBeanDefination() throws IOException {

        InputStream inputStream = ClassPathXmlApplicationContext.class.getClassLoader().getResourceAsStream(this.configPath);

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));//构造一个BufferedReader类来读取文件
        String line = null;

        Beans beans = null;


        String currentbeanName;
        Bean currentBean = null;

        while ((line = br.readLine()) != null) {//使用readLine方法，一次读一行
            if ("<beans>".equals(line)) {
                beans = new Beans();
            } else if (line.trim().startsWith("<bean")) {
                if (beans == null) {
                    throw new RuntimeException("配置文件格式错误，bean节点初始化必须存在beans节点！");
                }

                currentBean = new Bean();
                Pattern patternId = Pattern.compile("id=\"(.*?)\"");
                Matcher matcherId = patternId.matcher(line);
                while (matcherId.find()) {
                    String id = matcherId.group(1);
                    currentBean.setId(id);
                    currentbeanName = id;
                }
                Pattern patternBeanClass = Pattern.compile("class=\"(.*?)\"");
                Matcher matcherBeanClass = patternBeanClass.matcher(line);
                while (matcherBeanClass.find()) {
                    String beanClass = matcherBeanClass.group(1);
                    currentBean.setBeanClass(beanClass);
                }

                Pattern patternInitMethod = Pattern.compile("init-method=\"(.*?)\"");
                Matcher matcherInitMethod = patternInitMethod.matcher(line);
                while (matcherInitMethod.find()) {
                    String initMethodName = matcherInitMethod.group(1);
                    currentBean.setInitMethod(initMethodName);
                }

                Pattern patternScope = Pattern.compile("scope=\"(.*?)\"");
                Matcher matcherScope = patternScope.matcher(line);
                while (matcherScope.find()) {
                    String scope = matcherScope.group(1);
                    currentBean.setScope(scope);
                }


                beans.addNode(currentBean);

            } else if (line.trim().startsWith("<property")) {
                Pattern patternPropertity = Pattern.compile("name=\"(.*?)\"");
                Matcher matcherPropertity = patternPropertity.matcher(line);
                String propertyName = null;
                String propertyValue = null;
                String propertyRef = null;
                while (matcherPropertity.find()) {
                    propertyName = matcherPropertity.group(1);
                    if (currentBean == null) {
                        throw new RuntimeException("解析失败，property必须在bean范围内");
                    }
                }
                Pattern patternValue = Pattern.compile("value=\"(.*?)\"");
                Matcher matcherValue = patternValue.matcher(line);
                while (matcherValue.find()) {
                    propertyValue = matcherValue.group(1);
                }

                Pattern patternRef = Pattern.compile("ref=\"(.*?)\"");
                Matcher matcherRef = patternRef.matcher(line);
                while (matcherRef.find()) {
                    propertyRef = matcherRef.group(1);
                }
                currentBean.addProperty(propertyName, propertyValue, propertyRef);
            }

        }

        List<Bean> beanList = beans.getBeanList();
        Map<String, GenericBeanDefinition> beanDefinationMap = beanList.stream().collect(Collectors.toMap(Bean::getId, bean -> {
            GenericBeanDefinition beanDefination = new GenericBeanDefinition();
            try {
                BeanUtils.copyProperties(beanDefination, bean);
                bean.getProperty().forEach(property -> {
                    beanDefination.putDep(property.getName(), property.getRef());
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
            beanDefination.setScope(bean.getScope() == null ? "singleton" : bean.getScope());
            return beanDefination;
        }));

        return beanDefinationMap;
    }


}

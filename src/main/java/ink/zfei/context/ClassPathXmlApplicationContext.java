package ink.zfei.context;

import ink.zfei.core.AbstractApplicationContext;
import ink.zfei.core.BeanDefinition;
import ink.zfei.util.BeanUtils;
import ink.zfei.xmlParse.Bean;
import ink.zfei.xmlParse.Beans;

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
    protected Map<String, BeanDefinition> loadBeanDefination() throws IOException {

        InputStream inputStream = ClassPathXmlApplicationContext.class.getClassLoader().getResourceAsStream(this.configPath);

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));//构造一个BufferedReader类来读取文件
        String line = null;

        Beans beans = null;
        while ((line = br.readLine()) != null) {//使用readLine方法，一次读一行
            if ("<beans>".equals(line)) {
                beans = new Beans();
            } else if (line.trim().startsWith("<bean")) {
                if (beans == null) {
                    throw new RuntimeException("配置文件格式错误，bean节点初始化必须存在beans节点！");
                }
                Bean bean = new Bean();
                Pattern patternId = Pattern.compile("id=\"(.*?)\"");
                Matcher matcherId = patternId.matcher(line);
                while (matcherId.find()) {
                    String id = matcherId.group(1);
                    bean.setId(id);
                }
                Pattern patternBeanClass = Pattern.compile("class=\"(.*?)\"");
                Matcher matcherBeanClass = patternBeanClass.matcher(line);
                while (matcherBeanClass.find()) {
                    String beanClass = matcherBeanClass.group(1);
                    bean.setBeanClass(beanClass);
                }

                Pattern patternInitMethod = Pattern.compile("init-method=\"(.*?)\"");
                Matcher matcherInitMethod = patternInitMethod.matcher(line);
                while (matcherInitMethod.find()) {
                    String initMethodName = matcherInitMethod.group(1);
                    bean.setInitMethod(initMethodName);
                }

                Pattern patternScope = Pattern.compile("scope=\"(.*?)\"");
                Matcher matcherScope = patternScope.matcher(line);
                while (matcherScope.find()) {
                    String scope = matcherScope.group(1);
                    bean.setScope(scope);
                }
                beans.addNode(bean);

            }

        }

        List<Bean> beanList = beans.getBeanList();
        Map<String, BeanDefinition> beanDefinationMap = beanList.stream().collect(Collectors.toMap(Bean::getId, bean -> {
            BeanDefinition beanDefination = new BeanDefinition();
            try {
                BeanUtils.copyProperties(beanDefination, bean);
            } catch (Exception e) {
                e.printStackTrace();
            }
            beanDefination.setScope(bean.getScope() == null ? "singleton" : bean.getScope());
            return beanDefination;
        }));

        return beanDefinationMap;
    }


}

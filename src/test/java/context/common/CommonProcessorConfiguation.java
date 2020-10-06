package context.common;


import bean.Person;
import bean.PersonMapper;
import ink.zfei.summer.beans.factory.FactoryBean;
import ink.zfei.summer.core.annotation.Bean;
import ink.zfei.summer.core.annotation.Configuration;

@Configuration
public class CommonProcessorConfiguation {


    @Bean
    public LogBeanPostProcessor logBeanPostProcessor() {
        return new LogBeanPostProcessor();
    }

//    @Bean
//    public StarterBeanDefinitionPostProcessor starterBeanDefinitionPostProcessor() {
//        return new StarterBeanDefinitionPostProcessor();
//    }

    @Bean
    public Person person() {
        return new Person();
    }

    @Bean
    public FactoryBean personMapper() {
        return new MyFactoryBean();
    }

    public static class MyFactoryBean implements FactoryBean {

        @Override
        public Object getObject() {
            return new PersonMapper();
        }

        @Override
        public Class<?> getObjectType() {
            return PersonMapper.class;
        }
    }
}

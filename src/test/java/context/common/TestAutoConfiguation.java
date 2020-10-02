package context.common;


import bean.Water;
import bean.configuration.MyImportSelect;
import ink.zfei.summer.annation.Import;
import ink.zfei.summer.core.annation.Bean;
import ink.zfei.summer.core.annation.Configuration;

@Configuration
public class TestAutoConfiguation {


    @Bean
    public Water water() {
        return new Water();
    }


}

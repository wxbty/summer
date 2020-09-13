package ink.zfei.summer.demo;


import ink.zfei.summer.annation.Import;
import ink.zfei.summer.core.annation.Bean;
import ink.zfei.summer.core.annation.Configuration;

@Configuration
//@Import(MyImportSelect.class)
public class TestAutoConfiguation {


    @Bean
    public Water water() {
        return new Water();
    }


}

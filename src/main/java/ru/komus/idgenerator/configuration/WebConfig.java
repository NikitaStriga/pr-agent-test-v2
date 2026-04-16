package ru.komus.idgenerator.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

/**
 * Configuration of html template, swagger, git-info.
 */
@Configuration
public class WebConfig
{
    public final static String AUTH = "Authorization";
    public final static String BASIC = "Basic";
    public final static String NAME_BASIC_AUTH = BASIC + " " + AUTH;

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer()
    {
        PropertySourcesPlaceholderConfigurer propsConfig
            = new PropertySourcesPlaceholderConfigurer();
        propsConfig.setLocation(new ClassPathResource("git.properties"));
        return propsConfig;
    }

    @Bean
    public OpenAPI customOpenAPI()
    {
        SecurityScheme basicSecurity = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .name(AUTH)
            .in(SecurityScheme.In.HEADER)
            .scheme(BASIC);

        return new OpenAPI()
            .components(new Components().addSecuritySchemes(NAME_BASIC_AUTH, basicSecurity))
            .addSecurityItem(new SecurityRequirement().addList(NAME_BASIC_AUTH));
    }

    @Bean
    public ThymeleafViewResolver viewResolver(SpringTemplateEngine templateEngine)
    {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        return viewResolver;
    }

}

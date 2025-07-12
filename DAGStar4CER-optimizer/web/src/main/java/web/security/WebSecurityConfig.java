package web.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import web.component.MyBasicAuthenticationEntryPointComponent;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private MyBasicAuthenticationEntryPointComponent authenticationEntryPoint;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Disable CSRF (cross site request forgery)
        http.csrf().disable();

        // No session will be created or used by spring security
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // Entry points
        http.authorizeRequests()
                .antMatchers("/*").permitAll()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
                .and()
                .httpBasic()
                .authenticationEntryPoint(authenticationEntryPoint);
    }

    //Can pull credentials from a DB.
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser("infore_admin")
                .password(passwordEncoder().encode("infore_admin_pass"))
                .roles("ADMIN");

        auth.inMemoryAuthentication()
                .withUser("benchmarking_user")
                .password(passwordEncoder().encode("benchmarking_pass"))
                .roles("ADMIN");

        auth.inMemoryAuthentication()
                .withUser("bo_user")
                .password(passwordEncoder().encode("bo_pass"))
                .roles("ADMIN");

        auth.inMemoryAuthentication()
                .withUser("fs_user")
                .password(passwordEncoder().encode("fs_pass"))
                .roles("ADMIN");

        auth.inMemoryAuthentication()
                .withUser("adaptation_user")
                .password(passwordEncoder().encode("adaptation_pass"))
                .roles("ADMIN");

        auth.inMemoryAuthentication()
                .withUser("infore_user")
                .password(passwordEncoder().encode("infore_pass"))
                .roles("ADMIN");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

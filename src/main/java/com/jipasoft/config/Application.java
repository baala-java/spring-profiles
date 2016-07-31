/*
* Copyright 2016, Julius Krah
* by the @authors tag. See the LICENCE in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.jipasoft.config;

import java.util.Locale;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.mongobee.Mongobee;
import com.jipasoft.aop.exceptions.ExceptionAspect;
import com.jipasoft.domain.AbstractAuditEntity;
import com.jipasoft.service.Services;
import com.jipasoft.util.Profiles;
import com.jipasoft.web.Controllers;
import com.mongodb.Mongo;

import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;

/**
 * Application root configuration. The
 * {@link SpringBootApplication @SpringBootApplication} <br />
 * is a convenience annotation for {@link ComponentScan @ComponentScan},
 * {@link Configuration @Configuration}, and <br />
 * {@link EnableAutoConfiguration @EnableAutoConfiguration}. The
 * {@code scanBasePackageClasses} in this context is type safe.
 * 
 * @see H2Config
 * @see PostgresConfig
 * @see MySQLConfig
 * 
 * @author Julius Krah
 *
 */
@Slf4j
@EnableAspectJAutoProxy
@SpringBootApplication(scanBasePackageClasses = { Controllers.class, Services.class, ExceptionAspect.class })
@EnableConfigurationProperties({ LiquibaseProperties.class })
@EntityScan(basePackageClasses = AbstractAuditEntity.class)
@Import(value = { H2Config.class, PostgresConfig.class, MySQLConfig.class, MongoConfig.class, SecurityConfig.class })
public class Application extends WebMvcConfigurerAdapter {
	@Inject
	private LiquibaseProperties liquibaseProperties;
	@Inject
	private MongoProperties mongoProperties;
	@Inject
	private Mongo mongo;
	@Inject
	private Environment env;

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(Application.class);
		app.run(args);
	}

	@Bean
	public PasswordEncoder encoder() {
		return new BCryptPasswordEncoder(10);
	}

	/**
	 * i18n support bean
	 * 
	 * @return
	 */
	@Bean
	public LocaleResolver localeResolver() {
		CookieLocaleResolver slr = new CookieLocaleResolver();
		slr.setDefaultLocale(Locale.US);
		return slr;
	}

	/**
	 * i18n bean support for switching locales through a request param
	 * 
	 * @return
	 */
	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
		lci.setParamName("lang");
		return lci;
	}

	/**
	 * This bean was added to support JSR310 serialization to JSON
	 * 
	 * @param builder
	 * @return
	 */
	@Bean
	@Primary
	public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
		ObjectMapper objectMapper = builder.createXmlMapper(false).build();
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		// objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS,
		// false);
		return objectMapper;
	}

	/**
	 * SQL database migration
	 * 
	 * @param dataSource
	 * @return
	 */
	@Bean
	public SpringLiquibase liquibase(DataSource dataSource) {
		SpringLiquibase liquibase = new SpringLiquibase();
		liquibase.setDataSource(dataSource);
		liquibase.setChangeLog(liquibaseProperties.getChangeLog());
		liquibase.setContexts(liquibaseProperties.getContexts());
		liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
		liquibase.setDropFirst(liquibaseProperties.isDropFirst());
		if (env.acceptsProfiles(Profiles.MONGO))
			liquibase.setShouldRun(false);
		else {
			liquibase.setShouldRun(liquibaseProperties.isEnabled());
			log.trace("Configuring Liquibase...");
		}

		return liquibase;
	}

	/**
	 * Database migration
	 * 
	 * @return Mongobee
	 */
	@Bean
	@ConditionalOnExpression("#{environment.acceptsProfiles('" + Profiles.MONGO + "')}")
	public Mongobee mongobee() {
		log.trace("Configuring Mongobee...");
		Mongobee mongobee = new Mongobee(mongo);
		mongobee.setDbName(mongoProperties.getDatabase());
		// package to scan for migrations
		mongobee.setChangeLogsScanPackage("com.jipasoft.config.dbmigrations");
		// set environment to process @Profile on
		// 'com.jipasoft.config.dbmigrations.InitialSetupMigration'
		mongobee.setSpringEnvironment(env);
		mongobee.setEnabled(true);
		return mongobee;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/login").setViewName("signin");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(localeChangeInterceptor());
	}

}

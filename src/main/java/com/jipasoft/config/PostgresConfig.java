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

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.jipasoft.repository.postgres.BaseRepositoryImpl;
import com.jipasoft.util.Profiles;

/**
 * Configuration specific to the {@code postgres} and {@code mysql} profiles. The implementation of
 * JPA here is standard specification JPA. The underlying datastore is
 * PostgreSQL and/or MySQL
 * 
 * @author Julius Krah
 *
 */
@Configuration
@Profile({ Profiles.POSTGRES, Profiles.MYSQL })
@ComponentScan(basePackageClasses = BaseRepositoryImpl.class)
public class PostgresConfig {

}

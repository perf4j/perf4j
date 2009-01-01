/* Copyright (c) 2008-2009 HomeAway, Inc.
 * All rights reserved.  http://www.perf4j.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Provides the framework for exposing performance graphs through a servlet front-end. The concrete servlet classes to
 * use depends on whether you are using the log4j or java.util.logging framework. For example, if using the log4j
 * framework you would use classes from the {@link org.perf4j.log4j.servlet} package.
 */
package org.perf4j.servlet;
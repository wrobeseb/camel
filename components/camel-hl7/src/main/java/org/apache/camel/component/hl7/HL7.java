/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.hl7;

import ca.uhn.hl7v2.validation.ValidationContext;

import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.builder.ValueBuilder;

public final class HL7 {
    
    private HL7() {
        // Helper class
    }

    public static ValueBuilder terser(String expression) {
        return new ValueBuilder(new TerserExpression(expression));
    }

    public static Expression ack() {
        return new AckExpression();
    }

    public static Expression ack(AckCode code) {
        return new AckExpression(code);
    }

    public static Expression ack(AckCode code, String errorMessage, int errorCode) {
        return new AckExpression(code, errorMessage, errorCode);
    }

    public static Predicate messageConformsTo(ValidationContext validationContext) {
        return new ValidationContextPredicate(validationContext);
    }

    public static Predicate messageConformsTo(Expression expression) {
        return new ValidationContextPredicate(expression);
    }

}

/*

Copyright 2010, 2022 Google Inc. & OpenRefine contributors
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.model;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.InjectableValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.HasFields;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;
import com.google.refine.util.StringUtils;

public class Cell implements HasFields, Serializable {

    private static final long serialVersionUID = 7456683757764146620L;
    @JsonIgnore
    final public Serializable value;
    @JsonIgnore
    final public Recon recon;

    private static final Logger logger = LoggerFactory.getLogger(Cell.class);

    public Cell(Serializable value, Recon recon) {
        this.value = value;
        this.recon = recon;
    }

    @Override
    public Object getField(String name, Properties bindings) {
        if ("value".equals(name)) {
            return value;
        } else if ("recon".equals(name)) {
            return recon;
        } else if ("errorMessage".equals(name)) {
            return getErrorMessage();
        }
        return null;
    }

    @Override
    public boolean fieldAlsoHasFields(String name) {
        return "recon".equals(name);
    }

    @JsonProperty("e")
    @JsonInclude(Include.NON_NULL)
    public String getErrorMessage() {
        if (ExpressionUtils.isError(value)) {
            return ((EvalError) value).message;
        }
        return null;
    }

    @JsonProperty("t")
    @JsonInclude(Include.NON_NULL)
    public String getTypeString() {
        if (value instanceof OffsetDateTime || value instanceof LocalDateTime) {
            return "date";
        }
        return null;
    }

    @JsonProperty("v")
    @JsonInclude(Include.NON_NULL)
    public Object getValue() {
        if (value != null && !ExpressionUtils.isError(value)) {
            Instant instant = null;
            if (value instanceof OffsetDateTime) {
                instant = ((OffsetDateTime) value).toInstant();
            } else if (value instanceof LocalDateTime) {
                instant = ((LocalDateTime) value).toInstant(ZoneOffset.of("Z"));
            }

            if (instant != null) {
                return instant.toString();
            } else if (value instanceof Double
                    && (((Double) value).isNaN() || ((Double) value).isInfinite())) {
                // write as a string
                return value.toString();
            } else if (value instanceof Float
                    && (((Float) value).isNaN() || ((Float) value).isInfinite())) {
                return value.toString();
            } else if (value instanceof Boolean || value instanceof Number) {
                return value;
            } else {
                return value.toString();
            }
        } else {
            return null;
        }
    }

    /**
     * TODO - use JsonIdentityInfo on recon - implement custom resolver to tie it to a pool - figure it all out
     * 
     * @return
     */
    @JsonProperty("r")
    @JsonInclude(Include.NON_NULL)
    public String getReconIdString() {
        if (recon != null) {
            return Long.toString(recon.id);
        }
        return null;
    }

    public void save(Writer writer, Properties options) {
        try {
            Pool pool = (Pool) options.get("pool");
            if (pool != null && recon != null) {
                pool.pool(recon);
            }
            ParsingUtilities.saveWriter.writeValue(writer, this);
        } catch (IOException e) {
            logger.error("Error writing cell to writer", e);
        }
    }

    static public Cell loadStreaming(String s, Pool pool) throws Exception {
        InjectableValues injectableValues = new InjectableValues.Std()
                .addValue("pool", pool);
        return ParsingUtilities.mapper.setInjectableValues(injectableValues)
                .readValue(s, Cell.class);
    }

    @JsonCreator
    static public Cell deserialize(
            @JsonProperty("v") Object value,
            @JsonProperty("t") String type,
            @JsonProperty("r") String reconId,
            @JsonProperty("e") String error,
            @JacksonInject("pool") Pool pool) {
        Recon recon = null;
        if (reconId != null) {
            recon = pool.getRecon(reconId);
        }
        if ("date".equals(type)) {
            value = ParsingUtilities.stringToDate((String) value);
        }
        if (error != null) {
            value = new EvalError(error);
        }
        return new Cell((Serializable) value, recon);
    }

    /**
     * Return a deep copy of this object, making sure that if one instance is modified, the other isn't.
     */
    public Cell deepCopy() {
        return new Cell(value, recon == null ? null : recon.dup());
    }

    @Override
    public String toString() {
        // TODO this is kept like this for now, but it should rather be a string which makes the difference
        // between the cell and the value clear (i.e. not be the same string representation as the value)
        // and include a representation of the recon field too.
        // (toString() should not be exposed to the end user and is only meant to be used for logging/debugging
        // purposes)
        return StringUtils.toString(value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recon, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Cell other = (Cell) obj;
        return Objects.equals(recon, other.recon) && Objects.equals(value, other.value);
    }

}

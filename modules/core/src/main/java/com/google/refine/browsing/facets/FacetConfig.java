/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.browsing.facets;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.google.refine.model.Project;

/**
 * Represents the configuration of a facet, as stored in the engine configuration and in the JSON serialization of
 * operations. It does not contain the actual values displayed by the facet.
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = ListFacet.ListFacetConfig.class, name = "list"),
        @Type(value = RangeFacet.RangeFacetConfig.class, name = "range"),
        @Type(value = TimeRangeFacet.TimeRangeFacetConfig.class, name = "timerange"),
        @Type(value = TextSearchFacet.TextSearchFacetConfig.class, name = "text"),
        @Type(value = ScatterplotFacet.ScatterplotFacetConfig.class, name = "scatterplot") })
public interface FacetConfig {

    /**
     * Instantiates the given facet on a particular project.
     * 
     * @param project
     * @return a computed facet on the given project.
     */
    public Facet apply(Project project);

    /**
     * The facet type as stored in json.
     */
    @JsonIgnore // already included by @JsonTypeInfo
    public String getJsonType();

    /**
     * Checks that this facet is correctly configured (such as that expressions are syntactically correct and that
     * options are not contradictory). This should not be done in the constructor, as it would endanger the
     * deserialization.
     * 
     * @throws IllegalArgumentException
     *             if any parameter is missing or inconsistent
     */
    public default void validate() {
    }

    /**
     * Returns an approximation of the names of the columns this facet depends on. This approximation is designed to be
     * safe: if a set of column names is returned, then the expression does not read any other column than the ones
     * mentioned, regardless of the data it is executed on.
     *
     * @return {@link Optional#empty()} if the columns could not be isolated: in this case, the facet might depend on
     *         all columns in the project. Note that this is different from returning an empty set, which means that the
     *         facet's evaluation does not depend on any column.
     */
    @JsonIgnore
    public default Optional<Set<String>> getColumnDependencies() {
        return Optional.empty();
    }

    /**
     * Translates this facet by simultaneously substituting column names, as specified by the supplied map. This is a
     * best effort transformation: some references to columns might not get renamed in complex expressions. It can
     * generate an invalid facet configuration. Returning the same facet configuration is the default.
     *
     * @param substitutions
     *            a map specifying new names for some columns. Keys of the map are old column names, values are the new
     *            names for those columns. If a column name is not present in the map, the column is not renamed.
     * @return a new facet with updated column names. If this renaming isn't supported, the same facet config should be
     *         returned
     */
    public default FacetConfig renameColumnDependencies(Map<String, String> substitutions) {
        return this;
    }

}

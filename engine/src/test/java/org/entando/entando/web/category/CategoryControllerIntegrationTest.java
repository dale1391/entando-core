/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.web.category;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.FileTextReader;
import java.io.InputStream;
import org.entando.entando.aps.system.services.category.ICategoryService;
import org.entando.entando.web.AbstractControllerIntegrationTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

public class CategoryControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private ICategoryManager categoryManager;

    @Autowired
    @InjectMocks
    private CategoryController controller;

    @Test
    public void testGetCategories() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/categories")
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());
        System.out.println(result.andReturn().getResponse().getContentAsString());
        result.andExpect(header().string("Access-Control-Allow-Origin", "*"));
        result.andExpect(header().string("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS"));
        result.andExpect(header().string("Access-Control-Allow-Headers", "Content-Type, Authorization"));
        result.andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    public void testGetValidCategoryTree() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/categories")
                        .param("parentCode", "home")
                        .header("Authorization", "Bearer " + accessToken));
        System.out.println(result.andReturn().getResponse().getContentAsString());
        result.andExpect(status().isOk());
    }

    @Test
    public void testGetInvalidCategoryTree() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        this.executeGet("invalid_code", accessToken, status().isNotFound());
        ResultActions result = mockMvc
                .perform(get("/categories")
                        .param("parentCode", "invalid_code")
                        .header("Authorization", "Bearer " + accessToken));
        System.out.println(result.andReturn().getResponse().getContentAsString());
        result.andExpect(status().isNotFound());
    }

    @Test
    public void testGetValidCategory() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        this.executeGet("cat1", accessToken, status().isOk());
    }

    @Test
    public void testGetInvalidCategory() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        this.executeGet("invalid_code", accessToken, status().isNotFound());
    }

    @Test
    public void testAddCategory() throws Exception {
        String categoryCode = "test_cat";
        try {
            Assert.assertNotNull(this.categoryManager.getCategory("cat1"));
            Assert.assertNull(this.categoryManager.getCategory(categoryCode));
            UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
            String accessToken = mockOAuthInterceptor(user);
            this.executePost("1_POST_invalid_1.json", accessToken, status().isBadRequest());
            this.executePost("1_POST_invalid_2.json", accessToken, status().isBadRequest());
            this.executePost("1_POST_invalid_3.json", accessToken, status().isNotFound());
            this.executePost("1_POST_valid.json", accessToken, status().isOk());
            Assert.assertNotNull(this.categoryManager.getCategory(categoryCode));
            this.executeDelete(categoryCode, accessToken, status().isOk());
            Assert.assertNull(this.categoryManager.getCategory(categoryCode));
        } finally {
            this.categoryManager.deleteCategory(categoryCode);
        }
    }

    @Test
    public void testUpdateCategory() throws Exception {
        String categoryCode = "test_cat2";
        try {
            Assert.assertNotNull(this.categoryManager.getCategory("cat1"));
            Assert.assertNull(this.categoryManager.getCategory(categoryCode));
            UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
            String accessToken = mockOAuthInterceptor(user);
            this.executePost("2_POST_valid.json", accessToken, status().isOk());
            this.executePut("2_PUT_invalid_1.json", categoryCode, accessToken, status().isBadRequest());
            this.executePut("2_PUT_invalid_2.json", categoryCode, accessToken, status().isBadRequest());
            this.executePut("2_PUT_valid.json", "home", accessToken, status().isBadRequest());
            this.executePut("2_PUT_valid.json", categoryCode, accessToken, status().isOk());
            Category modified = this.categoryManager.getCategory(categoryCode);
            Assert.assertNotNull(modified);
            Assert.assertTrue(modified.getTitle("en").startsWith("New "));
            Assert.assertTrue(modified.getTitle("it").startsWith("Nuovo "));
            this.executeDelete(categoryCode, accessToken, status().isOk());
            Assert.assertNull(this.categoryManager.getCategory(categoryCode));
        } finally {
            this.categoryManager.deleteCategory(categoryCode);
        }
    }

    @Test
    public void testDeleteCategory() throws Exception {
        String categoryCode = "test_cat";
        try {
            Assert.assertNotNull(this.categoryManager.getCategory("cat1"));
            Assert.assertNull(this.categoryManager.getCategory(categoryCode));
            UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
            String accessToken = mockOAuthInterceptor(user);
            this.executePost("1_POST_valid.json", accessToken, status().isOk());
            Assert.assertNotNull(this.categoryManager.getCategory(categoryCode));
            this.executeDelete(categoryCode, accessToken, status().isOk());
            Assert.assertNull(this.categoryManager.getCategory(categoryCode));
            this.executeDelete("invalid_category", accessToken, status().isOk());
            this.executeDelete("cat1", accessToken, status().isBadRequest());
        } finally {
            this.categoryManager.deleteCategory(categoryCode);
        }
    }

    @Test
    public void testGetCategoryReferences() throws Exception {
        Assert.assertNotNull(this.categoryManager.getCategory("cat1"));
        Assert.assertNull(this.categoryManager.getCategory("test_test"));
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = this.mockOAuthInterceptor(user);
        this.executeReference("cat1", accessToken, SystemConstants.DATA_OBJECT_MANAGER, status().isOk());
        this.executeReference("test_test", accessToken, SystemConstants.DATA_OBJECT_MANAGER, status().isNotFound());
        this.executeReference("cat1", accessToken, SystemConstants.GROUP_MANAGER, status().isNotFound());
    }

    private void executeGet(String categoryCode, String accessToken, ResultMatcher rm) throws Exception {
        ResultActions result = mockMvc
                .perform(get("/categories/{categoryCode}", new Object[]{categoryCode})
                        .header("Authorization", "Bearer " + accessToken));
        System.out.println(result.andReturn().getResponse().getContentAsString());
        result.andExpect(rm);
    }

    private void executePost(String filename, String accessToken, ResultMatcher rm) throws Exception {
        InputStream isJsonPost = this.getClass().getResourceAsStream(filename);
        String jsonPost = FileTextReader.getText(isJsonPost);
        ResultActions result = mockMvc
                .perform(post("/categories").content(jsonPost)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(rm);
    }

    private void executePut(String filename, String categoryCode, String accessToken, ResultMatcher rm) throws Exception {
        InputStream isJsonPut = this.getClass().getResourceAsStream(filename);
        String jsonPut = FileTextReader.getText(isJsonPut);
        ResultActions result = mockMvc
                .perform(put("/categories/{categoryCode}", new Object[]{categoryCode})
                        .content(jsonPut)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(rm);
    }

    private void executeDelete(String categoryCode, String accessToken, ResultMatcher rm) throws Exception {
        ResultActions result = mockMvc
                .perform(delete("/categories/{categoryCode}", new Object[]{categoryCode})
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(rm);
    }

    private void executeReference(String categoryCode, String accessToken, String managerName, ResultMatcher rm) throws Exception {
        ResultActions result = mockMvc
                .perform(get("/categories/{categoryCode}/references/{holder}", new Object[]{categoryCode, managerName})
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(rm);
    }

}
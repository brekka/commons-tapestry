/*
 * Copyright 2012 the original author or authors.
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

package org.brekka.commons.tapestry.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.SupportsInformalParameters;
import org.apache.tapestry5.dom.Element;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Response;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * QR code component
 *
 * @author Andrew Taylor (andrew@brekka.org)
 */
@SupportsInformalParameters
public class QrCode {

    @Parameter(required=true)
    private String url;
    
    @Parameter
    private int size = 200;
    
    @Inject
    private ComponentResources componentResources;

    void beginRender(MarkupWriter writer) throws Exception {
        String base64urlSafeString = Base64.encodeBase64URLSafeString(url.getBytes("UTF-8"));
        Link link = componentResources.createEventLink("action", base64urlSafeString, size);
        Element img = writer.element("img", "src", link.toURI(), "alt", "QR Code");
        List<String> informalParameterNames = componentResources.getInformalParameterNames();
        for (String param : informalParameterNames) {
            String value = componentResources.getInformalParameter(param, String.class);
            img.attribute(param, value);
        }
        writer.end();
    }
    
    Object onAction(String base64Url, int size) throws Exception {
        byte[] decodeBase64 = Base64.decodeBase64(base64Url);
        String url = new String(decodeBase64, "UTF-8");
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix;
        try {
            matrix = writer.encode(url, BarcodeFormat.QR_CODE, size, size);
        } catch (WriterException e) {
            throw new IOException(e);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "png", baos);
        
        baos.close();
        final byte[] result = baos.toByteArray();
        
        return new StreamResponse() {
            
            @Override
            public void prepareResponse(Response response) {
                response.setContentLength(result.length);
            }
            
            @Override
            public InputStream getStream() throws IOException {
                return new ByteArrayInputStream(result);
            }
            
            @Override
            public String getContentType() {
                return "image/png";
            }
        };
    }
}

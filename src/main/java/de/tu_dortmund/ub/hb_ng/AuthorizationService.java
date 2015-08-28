/*
The MIT License (MIT)

Copyright (c) 2015, Hans-Georg Becker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package de.tu_dortmund.ub.hb_ng;

import de.tu_dortmund.ub.data.ldp.auth.AuthorizationException;
import de.tu_dortmund.ub.data.ldp.auth.AuthorizationInterface;
import de.tu_dortmund.ub.util.AEScrypter;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Properties;

/**
 * Linked Data Platform de.tu_dortmund.ub.hb_ng.data.AuthorizationService
 *
 * @author Dipl.-Math. Hans-Georg Becker, M.L.I.S. (UB Dortmund)
 * @version 2015-08-07
 *
 */
public class AuthorizationService implements AuthorizationInterface {

    private Properties config;
    private Logger logger;

    Properties apikeys;

    @Override
    public void init(Properties properties) {

        this.config = properties;
        PropertyConfigurator.configure(this.config.getProperty("service.log4j-conf"));
        this.logger = Logger.getLogger(AuthorizationService.class.getName());

        // Init ApiKeys
        if (this.config.getProperty("service.auth.apikeys") != null && !this.config.getProperty("service.auth.apikeys").equals("")) {

            try {

                String password = this.config.getProperty("service.id");

                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(this.config.getProperty("service.auth.apikeys")), "UTF-8"));

                String decryptedApiKeys = "";
                String line = "";

                while ((line = in.readLine()) != null) {

                    decryptedApiKeys += line.trim();
                }

                in.close();

                AEScrypter aesCrypter = new AEScrypter(password);
                String encryptedApiKeys = aesCrypter.AEStoString(decryptedApiKeys);

                this.apikeys = new Properties();
                this.apikeys.load(new StringReader(encryptedApiKeys));

                this.logger.info("[" + this.config.getProperty("service.name") + "] " + "Api-Keys loaded!");

                for (String key : this.apikeys.stringPropertyNames()) {

                    this.logger.debug("[" + this.config.getProperty("service.name") + "] " + key + " - " + this.apikeys.getProperty(key));
                }

            }
            catch (Exception e) {

                this.logger.warn("[" + this.config.getProperty("service.name") + "] " + "Api-Keys not supported! Could not read key file!");
            }
        }
        else {

            this.logger.info("[" + this.config.getProperty("service.name") + "] " + "Api-Keys not supported!");
        }
    }

    /**
     *
     * @param service
     * @param patronid
     * @param access_token
     * @return
     * @throws AuthorizationException
     */
    @Override
    public boolean isTokenValid(HttpServletResponse httpServletResponse, String service, String patronid, String access_token) throws AuthorizationException {

        boolean isAuthorized = false;

        String scope = "";

        switch (service) {

            case "data" : {

                scope = "read_full_data";
                break;
            }
        }

        // if !isAuthorized: valid api_key?
        if (!isAuthorized && this.apikeys != null) {

            String token = "";

            if (access_token.contains(" ")) {

                token = access_token.split(" ")[1];
            }
            else {
                token = access_token;
            }

            if (this.apikeys.containsKey(token) && this.apikeys.getProperty(token).contains(scope)) {

                isAuthorized = true;
                httpServletResponse.setHeader("X-OAuth-Scopes", scope);
            }
            else {

                isAuthorized = false;
            }
        }

        if (!isAuthorized && this.config.getProperty("service.test.key") != null && access_token.equals(this.config.getProperty("service.test.key"))) {

            isAuthorized = true;
        }

        return isAuthorized;
    }
}

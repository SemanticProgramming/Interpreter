/*
 * Created by Roman Baum on 24.09.15.
 * Last modified by Roman Baum on 22.01.19.
 */

package soccomas.basic;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * The class "MDBURLEncoder" provides one default constructor and one methods. The method "encodeUrl" encode an URL
 * with an arbitrary encoding scheme (e.g. "UTF-8").
 */
public class SOCCOMASURLEncoder {


    public SOCCOMASURLEncoder() {
    }

    /**
     * The method encode the in input URI with an encoding scheme
     * @param uri an URI-String to encode
     * @param encodingScheme this encoding scheme is used to encode the URI
     * @return an encoded URI
     */
    public String encodeUrl(String uri, String encodingScheme) {

        // differ between "normal" strings and typical parts of a URI
        if (uri.contains("://") & uri.contains(".") & uri.contains("/") ) {

            String uriParts[] = uri.split("://");

            String protocol = uriParts[0];

            String restOfUri = uriParts[1];

            uriParts = restOfUri.split("\\.");

            // todo simplify this if branch in the future
            if (uriParts.length == 4) {

                //for the all except last token of host
                for (int i = 0; i < uriParts.length - 1; i++) {

                    try {

                        if (uriParts[i].contains("/")) {

                            String uriSubParts[] = uriParts[i].split("/");

                            uriParts[i] = "";

                            for (int j = 0; j < uriSubParts.length; j++) {

                                if (j == (uriSubParts.length - 1) ) {

                                    uriParts[i] += URLEncoder.encode(uriSubParts[j], encodingScheme);

                                } else {

                                    uriParts[i] += URLEncoder.encode(uriSubParts[j], encodingScheme) + "/";

                                }

                            }

                        } else {

                            uriParts[i] = URLEncoder.encode(uriParts[i], encodingScheme);

                        }


                    } catch (UnsupportedEncodingException e) {

                        e.printStackTrace();

                    }
                }

                String uriParts2[] = uriParts[uriParts.length - 2].split("/");

                String host = "";

                // process the dot part of the URI
                for (int i = 0; i < uriParts.length - 2; i++) {

                    if ((uriParts.length - 2 >= 2) && (i == 0)) {

                        host = host + uriParts[i] + ".";

                    } else {

                        host = host + uriParts[i];

                    }

                }

                try {

                    host = host + "." + URLEncoder.encode(uriParts2[0], encodingScheme);

                } catch (UnsupportedEncodingException e) {

                    e.printStackTrace();

                }


                host = host.substring(0);

                String remainingPart = "";

                // process the slash part of the URI
                for (int i = 1; i < uriParts2.length; i++) {

                    try {

                        remainingPart = remainingPart + "/" + URLEncoder.encode(uriParts2[i], encodingScheme);

                    } catch (UnsupportedEncodingException e) {

                        e.printStackTrace();

                    }

                }

                String fragment = "";

                if (uriParts[uriParts.length -1].contains("#")) {

                    String uriParts3[] = uriParts[uriParts.length -1].split("#");

                    try {

                        fragment = URLEncoder.encode(uriParts3[0], encodingScheme) + "#" + URLEncoder.encode(uriParts3[1], encodingScheme);

                    } catch (UnsupportedEncodingException e) {

                        e.printStackTrace();

                    }

                } else {

                    try {

                        fragment = URLEncoder.encode(uriParts[uriParts.length -1], encodingScheme);

                    } catch (UnsupportedEncodingException e) {

                        e.printStackTrace();

                    }

                }

                return (protocol + "://" + host + remainingPart + "." + fragment);




            } else {

                //for the all except last token of host
                for (int i = 0; i < uriParts.length - 1; i++) {

                    try {

                        if (uriParts[i].contains("/")) {

                            String uriSubParts[] = uriParts[i].split("/");

                            uriParts[i] = "";

                            for (int j = 0; j < uriSubParts.length; j++) {

                                if (j == (uriSubParts.length - 1) ) {

                                    uriParts[i] += URLEncoder.encode(uriSubParts[j], encodingScheme);

                                } else {

                                    uriParts[i] += URLEncoder.encode(uriSubParts[j], encodingScheme) + "/";

                                }

                            }

                        } else {

                            uriParts[i] = URLEncoder.encode(uriParts[i], encodingScheme);

                        }


                    } catch (UnsupportedEncodingException e) {

                        e.printStackTrace();

                    }
                }

                String uriParts2[] = uriParts[uriParts.length - 1].split("/");

                String host = "";

                // process the dot part of the URI
                for (int i = 0; i < uriParts.length - 1; i++) {

                    if ((uriParts.length - 1 >= 2) && (i == 0)) {

                        host = host + uriParts[i] + ".";

                    } else {

                        host = host + uriParts[i];

                    }

                }

                try {

                    host = host + "." + URLEncoder.encode(uriParts2[0], encodingScheme);

                } catch (UnsupportedEncodingException e) {

                    e.printStackTrace();

                }


                host = host.substring(0);

                String remainingPart = "";

                // process the slash part of the URI
                for (int i = 1; i < uriParts2.length; i++) {

                    try {

                        remainingPart = remainingPart + "/" + URLEncoder.encode(uriParts2[i], encodingScheme);

                    } catch (UnsupportedEncodingException e) {

                        e.printStackTrace();

                    }

                }

                return (protocol + "://" + host + remainingPart);

            }



        } else if (uri.contains(":") & uri.contains("@") & uri.contains(".")) {
            // mail uri

            String uriParts[] = uri.split(":");

            uriParts = uriParts[1].split("@");

            String beforeAt = null;

            try {

                beforeAt = URLEncoder.encode(uriParts[0], encodingScheme);

            } catch (UnsupportedEncodingException e) {

                e.printStackTrace();

            }

            uriParts = uriParts[1].split("\\.");

            String beforeDot = null;

            try {

                beforeDot = URLEncoder.encode(uriParts[0], encodingScheme);

            } catch (UnsupportedEncodingException e) {

                e.printStackTrace();

            }

            String afterDot = null;

            try {

                afterDot = URLEncoder.encode(uriParts[1], encodingScheme);

            } catch (UnsupportedEncodingException e) {

                e.printStackTrace();

            }


            return (beforeAt + "@" + beforeDot + "." + afterDot);


        } else {

            return ("The input is no URI!");

        }
    }

}

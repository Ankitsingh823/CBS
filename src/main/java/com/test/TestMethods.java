package com.test;

import com.utils.GenricMethods;
import com.model.ConfigurableBlock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class TestMethods {

    @Autowired
    private GenricMethods genricMethods;

    static final String SR_ROUTING = "SR_BASED_ROUTING"; // bool
    static final String INT_CONFIG = "MAX_RETRY_LIMIT";  // int
    static final String LIST_CONFIG = "ALLOWED_DOMAINS"; // list
    static final String JSON_CONFIG = "DEFAULT_OPTIMIZATION_ROUTING_CONFIG"; // json object

    @PostConstruct
    public void run() {
        System.out.println("===== Functional Config Test =====");

        // ✅ Test Boolean Config
        try {
            Boolean srRouting = genricMethods.getBooleanValue(SR_ROUTING);
            if (srRouting) {
                System.out.println(SR_ROUTING + ": true");
            } else {
                System.out.println(SR_ROUTING + ": false");
            }
        } catch (Exception e) {
            System.out.println(SR_ROUTING + ": SC_Doesn't_Exist");
        }

        // ✅ Test Integer Config
        try {
            Integer retryLimit = genricMethods.getIntegerValue(INT_CONFIG);
            System.out.println(INT_CONFIG + ": " + retryLimit);
        } catch (Exception e) {
            System.out.println(INT_CONFIG + ": SC_Doesn't_Exist");
        }

        // ✅ Test List Config
        try {
            List<String> domains = genricMethods.getListValue(LIST_CONFIG);
            System.out.println(LIST_CONFIG + ": " + domains);
        } catch (Exception e) {
            System.out.println(LIST_CONFIG + ": SC_Doesn't_Exist");
        }

        // ✅ Test JSON Object Config
        try {
            ConfigurableBlock block = genricMethods.getConfigAsObject(JSON_CONFIG, ConfigurableBlock.class);
            System.out.println(JSON_CONFIG + ": " + block);
        } catch (Exception e) {
            System.out.println(JSON_CONFIG + ": SC_Doesn't_Exist");
        }

        final String ROLLOUT_CONFIG = "FEATURE_X_ROLLOUT"; // rollout based config
        try {
            boolean rolloutEnabled = genricMethods.isRolloutEnabled(ROLLOUT_CONFIG, "merchant123");
            System.out.println(ROLLOUT_CONFIG + " for merchant123: " + rolloutEnabled);
        } catch (Exception e) {
            System.out.println(ROLLOUT_CONFIG + ": SC_Doesn't_Exist");
        }


        System.out.println("===== Test Complete =====");
    }
}

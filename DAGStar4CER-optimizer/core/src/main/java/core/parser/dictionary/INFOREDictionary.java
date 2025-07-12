package core.parser.dictionary;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.parser.network.INFORENetwork;
import core.parser.network.Site;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class INFOREDictionary implements Serializable {
    private final Map<String, List<Integer>> costCoefficients;
    private final Map<String, Integer> inputRate;
    //Platforms
    private final Map<String, Map<String, String>> operatorPlatformOperatorNames;
    private final Map<String, Map<String, Integer>> operatorPlatformStaticCosts;
    private final Map<String, Map<String, Map<String, Integer>>> operatorPlatformMigrationCosts;
    //Sites
    private final Map<String, Map<String, Integer>> operatorSiteStaticCosts;
    private final Map<String, Map<String, Map<String, Integer>>> operatorSiteMigrationCosts;
    private final Map<String, Map<String, List<String>>> operatorImplementations;
    private String name;
    //Original input
    private final String originalInput;

//    public INFOREDictionary(String input) {
//        //Save the input
//        this.originalInput = String.valueOf(input.toCharArray());
//
//        //Standard JSON parser
//        JsonParser jsonParser = new JsonParser();
//
//        //Init collections
//        this.costCoefficients = new HashMap<>();
//        this.inputRate = new HashMap<>();
//        this.operatorPlatformOperatorNames = new HashMap<>();
//        this.operatorPlatformStaticCosts = new HashMap<>();
//        this.operatorPlatformMigrationCosts = new HashMap<>();
//        this.operatorSiteStaticCosts = new HashMap<>();
//        this.operatorSiteMigrationCosts = new HashMap<>();
//
//        JsonObject element = jsonParser.parse(input).getAsJsonObject();
//        this.name = element.get("dictionaryName").getAsString();
//        for (JsonElement operatorElement : element.get("operators").getAsJsonArray()) {
//
//            //Class key for this operator
//            String opClassKey = operatorElement.getAsJsonObject().get("classKey").getAsString();
//
//            //Input rate (tuples/sec)
//            int inputRate = operatorElement.getAsJsonObject().get("inputRate").getAsInt();
//            this.inputRate.put(opClassKey, inputRate);
//
//            //Cost coefficients
//            this.costCoefficients.put(opClassKey, new ArrayList<>());
//            operatorElement.getAsJsonObject().get("costCoefficients").getAsJsonArray().iterator().forEachRemaining(i -> this.costCoefficients.get(opClassKey).add(i.getAsInt()));
//
//            //Platforms
//            this.operatorPlatformOperatorNames.put(opClassKey, new HashMap<>());
//            this.operatorPlatformStaticCosts.put(opClassKey, new HashMap<>());
//            this.operatorPlatformMigrationCosts.put(opClassKey, new HashMap<>());
//
//            Set<Map.Entry<String, JsonElement>> platformEntries = operatorElement.getAsJsonObject().get("platforms").getAsJsonObject().entrySet();
//            for (Map.Entry<String, JsonElement> platformEntry : platformEntries) {
//                String platformName = platformEntry.getKey();
//                JsonObject platformObject = platformEntry.getValue().getAsJsonObject();
//
//                String operatorName = platformObject.get("operatorName").getAsString();
//                this.operatorPlatformOperatorNames.get(opClassKey).put(platformName, operatorName);
//
//                int staticCost = platformObject.get("staticCost").getAsInt();
//                operatorPlatformStaticCosts.get(opClassKey).put(platformName, staticCost);
//
//                Map<String, Integer> migrationCostMap = new HashMap<>();
//                Set<Map.Entry<String, JsonElement>> migrationEntries = platformObject.get("migrationCosts").getAsJsonObject().entrySet();
//                for (Map.Entry<String, JsonElement> migrationEntry : migrationEntries) {
//                    String migrationPlatformName = migrationEntry.getKey();
//                    int migrationPlatformCost = migrationEntry.getValue().getAsInt();
//                    migrationCostMap.put(migrationPlatformName, migrationPlatformCost);
//                }
//                this.operatorPlatformMigrationCosts.get(opClassKey).put(platformName, migrationCostMap);
//            }
//
//            //Sites
//            this.operatorSiteStaticCosts.put(opClassKey, new HashMap<>());
//            this.operatorSiteMigrationCosts.put(opClassKey, new HashMap<>());
//            Set<Map.Entry<String, JsonElement>> siteEntries = operatorElement.getAsJsonObject().get("sites").getAsJsonObject().entrySet();
//            for (Map.Entry<String, JsonElement> siteEntry : siteEntries) {
//                String siteName = siteEntry.getKey();
//                JsonObject siteObject = siteEntry.getValue().getAsJsonObject();
//
//                int staticCost = siteObject.get("staticCost").getAsInt();
//                this.operatorSiteStaticCosts.get(opClassKey).put(siteName, staticCost);
//
//                Map<String, Integer> migrationCostMap = new HashMap<>();
//                Set<Map.Entry<String, JsonElement>> migrationEntries = siteObject.get("migrationCosts").getAsJsonObject().entrySet();
//                for (Map.Entry<String, JsonElement> migrationEntry : migrationEntries) {
//                    String migrationSiteName = migrationEntry.getKey();
//                    int migrationSiteCost = migrationEntry.getValue().getAsInt();
//                    migrationCostMap.put(migrationSiteName, migrationSiteCost);
//                }
//                this.operatorSiteMigrationCosts.get(opClassKey).put(siteName, migrationCostMap);
//            }
//        }
//        // Xenia from exp22
//        // Because RM sometimes forget things
//        this.patchOperators(Lists.newArrayList("streaming:union"));
//    }

    public INFOREDictionary(String input) {
        /* ---------- keep a copy of the raw JSON ---------- */
        this.originalInput = input;          // simpler than the old String.valueOf

        /* ---------- standard Gson setup ---------- */
        JsonParser jsonParser = new JsonParser();
        JsonObject root        = jsonParser.parse(input).getAsJsonObject();

        /* ---------- initialise all internal maps ---------- */
        this.costCoefficients              = new HashMap<>();
        this.inputRate                     = new HashMap<>();
        this.operatorPlatformOperatorNames = new HashMap<>();
        this.operatorPlatformStaticCosts   = new HashMap<>();
        this.operatorPlatformMigrationCosts= new HashMap<>();
        this.operatorSiteStaticCosts       = new HashMap<>();
        this.operatorSiteMigrationCosts    = new HashMap<>();
        this.operatorImplementations       = new HashMap<>();

        /* ---------- dictionary name ---------- */
        this.name = root.get("dictionaryName").getAsString();

        /* ---------- iterate over operators ---------- */
        for (JsonElement opElem : root.getAsJsonArray("operators")) {

            JsonObject  opObj     = opElem.getAsJsonObject();
            String      opKey     = opObj.get("classKey").getAsString();

            /* -------- core operator fields -------- */
            this.inputRate.put(opKey, opObj.get("inputRate").getAsInt());

            List<Integer> coeffs = new ArrayList<>();
            opObj.getAsJsonArray("costCoefficients")
                    .forEach(e -> coeffs.add(e.getAsInt()));
            this.costCoefficients.put(opKey, coeffs);

            /* ---------- prepare nested maps ---------- */
            this.operatorPlatformOperatorNames.put(opKey, new HashMap<>());
            this.operatorPlatformStaticCosts  .put(opKey, new HashMap<>());
            this.operatorPlatformMigrationCosts.put(opKey, new HashMap<>());
            this.operatorSiteStaticCosts.put(opKey, new HashMap<>());
            this.operatorSiteMigrationCosts.put(opKey, new HashMap<>());
            this.operatorImplementations.put(opKey, new HashMap<>());

        /* ==========================================================
           S I T E S   (array now, each element is { "<siteName>" : { … } })
           ========================================================== */
            JsonArray sitesArr = opObj.getAsJsonArray("sites");
            for (JsonElement siteWrapperElem : sitesArr) {

                JsonObject siteWrapper = siteWrapperElem.getAsJsonObject();
                /* each wrapper contains exactly one entry: <siteName> : {…} */
                for (Map.Entry<String, JsonElement> siteEntry : siteWrapper.entrySet()) {

                    String     siteName = siteEntry.getKey();
                    JsonObject siteObj  = siteEntry.getValue().getAsJsonObject();

                    /* ---- site-level for the operator implementation map ---- */
                    this.operatorImplementations.get(opKey).put(siteName, new ArrayList<>());

                    /* ---- site-level static & migration costs ---- */
                    int siteStatic = siteObj.get("staticCost").getAsInt();
                    this.operatorSiteStaticCosts.get(opKey).put(siteName, siteStatic);

                    Map<String,Integer> siteMigCosts = new HashMap<>();
                    for (Map.Entry<String,JsonElement> mc
                            : siteObj.getAsJsonObject("migrationCosts").entrySet()) {
                        siteMigCosts.put(mc.getKey(), mc.getValue().getAsInt());
                    }
                    this.operatorSiteMigrationCosts.get(opKey).put(siteName, siteMigCosts);

                /* ====================================================
                   P L A T F O R M S  (array, inside each site)
                   ==================================================== */
                    JsonArray platformsArr = siteObj.getAsJsonArray("platforms");
                    for (JsonElement platWrapperElem : platformsArr) {
                        JsonObject platWrapper = platWrapperElem.getAsJsonObject();
                        /* again exactly one child entry */
                        for (Map.Entry<String,JsonElement> platEntry : platWrapper.entrySet()) {

                            String     platformName = platEntry.getKey();
                            JsonObject platformObj  = platEntry.getValue().getAsJsonObject();

                            /* ---- platform-level for the operator implementation map ---- */
                            this.operatorImplementations.get(opKey)
                                    .get(siteName).add(platformName);

                            /* record operator name on that platform */
                            String opPlatformName =
                                    platformObj.get("operatorName").getAsString();
                            this.operatorPlatformOperatorNames
                                    .get(opKey)
                                    .put(platformName, opPlatformName);

                            /* static cost on the platform */
                            int platformStatic = platformObj.get("staticCost").getAsInt();
                            this.operatorPlatformStaticCosts
                                    .get(opKey)
                                    .put(platformName, platformStatic);

                            /* migration costs between platforms */
                            Map<String,Integer> platMigCosts = new HashMap<>();
                            for (Map.Entry<String,JsonElement> mc
                                    : platformObj.getAsJsonObject("migrationCosts").entrySet()) {
                                platMigCosts.put(mc.getKey(), mc.getValue().getAsInt());
                            }
                            this.operatorPlatformMigrationCosts
                                    .get(opKey)
                                    .put(platformName, platMigCosts);
                        } // platform wrapper entry
                    } // platforms array

                    // If there is no platform available for this operator on this site, then remove the site entry
                    if (this.operatorImplementations.get(opKey).get(siteName).isEmpty()) {
                        this.operatorImplementations.get(opKey).remove(siteName);
                    }
                } // site wrapper entry
            } // sites array
        } // operators
    }


    // Xenia from exp22
    public void patchOperators(Collection<String> opsToPatch) {
        Set<String> allOperators = this.costCoefficients.keySet();
        Set<String> allSites = this.operatorSiteStaticCosts.values().iterator().next().keySet();

        for (String opName : opsToPatch) {
            //Skip operator if it exists
            if (allOperators.contains(opName)) {
                continue;
            }

            List<Integer> ccList = new ArrayList<>();
            ccList.add(0);
            ccList.add(0);
            ccList.add(1);
            ccList.add(0);
            this.costCoefficients.put(opName, ccList);

            this.inputRate.put(opName, 200);

            Map<String, String> opon = new HashMap<>();
            opon.put("flink", String.format("flink_%s", opName));
            this.operatorPlatformOperatorNames.put(name, opon);

            Map<String, Integer> opsc = new HashMap<>();
            opsc.put("flink", 10);
            this.operatorPlatformStaticCosts.put(opName, opsc);

            Map<String, Map<String, Integer>> mig1 = new HashMap<>();
            Map<String, Integer> mig2 = new HashMap<>();
            mig2.put("flink", 0);
            mig1.put("flink", mig2);
            this.operatorPlatformMigrationCosts.put(opName, mig1);

            Map<String, Integer> static1 = new HashMap<>();
            for (String site : allSites) {
                static1.put(site, 100);
            }
            this.operatorSiteStaticCosts.put(opName, static1);

            Map<String, Map<String, Integer>> siteMigC = new HashMap<>();
            for (String site1 : allSites) {
                Map<String, Integer> innerMap = new HashMap<>();
                for (String site2 : allSites) {
                    if (site1.equals(site2)) {
                        innerMap.put(site2, 0);
                    } else {
                        innerMap.put(site2, 500);
                    }
                }
                siteMigC.put(site1, innerMap);
            }
            this.operatorSiteMigrationCosts.put(opName, siteMigC);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, List<String>> getImplementationsForClassKey(String operatorClassKey) {
        if (operatorClassKey == null) {
            throw new IllegalArgumentException("operatorClassKey cannot be null");
        }
        return this.operatorImplementations.get(operatorClassKey);
    }

    public Map<String, List<String>> getImplementationsForClassKey(String operatorClassKey, INFORENetwork network) {

//        for (Map.Entry<String, Map<String, List<String>>> impl : this.operatorImplementations.entrySet()) {
//            System.out.println("Operator: " + impl.getKey());
//            for (Map.Entry<String, List<String>> entry : impl.getValue().entrySet()) {
//                System.out.println("  Site: " + entry.getKey() + ", Platforms: " + String.join(", ", entry.getValue()));
//            }
//        }
//        System.out.println();

//        System.out.println("Getting implementations for operator class key: " + operatorClassKey);

        Map<String, List<String>> filteredImplementations = new HashMap<>();
        Set<String> availableSites = network.getSites().stream().map(Site::getSiteName).collect(Collectors.toSet());
        for (Map.Entry<String, List<String>> entry : this.operatorImplementations.get(operatorClassKey).entrySet()) {
            String site = entry.getKey();
            if (availableSites.contains(site)) {
                filteredImplementations.put(site, entry.getValue());
            }
        }
        return filteredImplementations;
    }

    //Dictionary API
//    public Map<String, List<String>> getImplementationsForClassKey(String operatorClassKey, INFORENetwork network) {
//        if (operatorClassKey == null) { //Xenia: Added null check
//            throw new IllegalArgumentException("operatorClassKey cannot be null");
//        }
//
//        if (network == null) { //Xenia: Added null check
//            throw new IllegalArgumentException("network cannot be null");
//        }
//
//        Map<String, List<String>> impls = new HashMap<>();
//        Set<String> supportedSites = network.getSites().stream().map(Site::getSiteName).collect(Collectors.toSet());
//
//        //Added by Xenia: null check for operatorPlatformStaticCosts because it is not guaranteed to be present
//        Map<String, Integer> siteStaticCosts = this.operatorSiteStaticCosts.get(operatorClassKey);
//        if (siteStaticCosts == null) {
//            throw new IllegalArgumentException("No site static costs found for operatorClassKey: " + operatorClassKey);
//        }
//
//        // TODO: Added in order to run the riot workflows without any pinned operators
////        for (String site : supportedSites) {
////            List<String> platforms = new ArrayList<>(this.operatorPlatformStaticCosts.getOrDefault(operatorClassKey, Collections.emptyMap()).keySet());
////            impls.put(site, platforms);
////        }
////        return impls;
//
//        //Above lines adapted by Xenia to include null check
//        for (String site : siteStaticCosts.keySet()) {
//            if (!supportedSites.contains(site)) {
//                continue;
//            }
//            Map<String, Integer> platformStaticCosts = this.operatorPlatformStaticCosts.get(operatorClassKey);
//            if (platformStaticCosts == null) {
//                throw new IllegalArgumentException("No platform static costs found for operatorClassKey: " + operatorClassKey + "impls are:" + impls);
//            }
//            List<String> platforms = new ArrayList<>(platformStaticCosts.keySet());
//            impls.put(site, platforms);
//        }
//
//        return impls;
//    }

    public int getOperatorCost(String classKey) {

        if (classKey == null) { //Xenia: Added null check
            throw new IllegalArgumentException("Class key cannot be null");
        }

        int inputRate = this.inputRate.get(classKey);
        int cost = 0;
        int coefficient = 0;
        for (int coefficientVal : this.costCoefficients.get(classKey)) {
            if (coefficient < 0) {
                throw new IllegalStateException("Illegal coefficient value: " + coefficientVal);
            } else if (coefficient == 0) {
                cost += coefficientVal;
            } else if (coefficient == 1) {
                cost += Math.log(inputRate) * coefficientVal;
            } else {
                cost += Math.pow(inputRate, coefficient - 1) * coefficientVal;
            }
            coefficient++;
        }
        return cost;
    }

    public int getMinSiteCostForOperator(String classKey) {
        if (classKey == null) { //Xenia: Added null check
            throw new IllegalArgumentException("Class key cannot be null");
        }

        return this.operatorSiteStaticCosts.getOrDefault(classKey, Collections.emptyMap()).entrySet().stream()
                .min(Comparator.comparingInt(Map.Entry::getValue))
                .orElseGet(() -> new AbstractMap.SimpleEntry<>(null, 0))
                .getValue();
    }

    public int getMinPlatformCostForOperator(String classKey) {

        if (classKey == null) { //Xenia: Added null check
            throw new IllegalArgumentException("Class key cannot be null");
        }

        return this.operatorPlatformStaticCosts.getOrDefault(classKey, Collections.emptyMap()).entrySet().stream()
                .min(Comparator.comparingInt(Map.Entry::getValue))
                .orElseGet(() -> new AbstractMap.SimpleEntry<>(null, 0))
                .getValue();
    }

    public Map<String, List<Integer>> getCostCoefficients() {
        return this.costCoefficients;
    }

    public Map<String, Integer> getInputRate() {
        return this.inputRate;
    }

    public int getMinPlatformCostMigrationForOperator(String classKey) {

        if (classKey == null) { //Xenia: Added null check
            throw new IllegalArgumentException("Class key cannot be null");
        }

        return this.operatorPlatformMigrationCosts.getOrDefault(classKey, Collections.emptyMap()).values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .min(Comparator.comparingInt(o -> o))
                .orElse(0);
    }

    public int getMinSiteCostMigrationForOperator(String classKey) {
        return this.operatorPlatformMigrationCosts.getOrDefault(classKey, Collections.emptyMap()).values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .min(Comparator.comparingInt(o -> o))
                .orElse(0);
    }

    public int getSiteMigrationCostForClassKey(String classKey, String fromSiteName, String toSiteName) {
        return this.operatorSiteMigrationCosts.get(classKey).get(fromSiteName).get(toSiteName);
    }

    public int getPlatformMigrationCostForClassKey(String classKey, String fromPlatformName, String toPlatformName) {
        return this.operatorPlatformMigrationCosts.get(classKey).get(fromPlatformName).get(toPlatformName);
    }

    public int getSiteStaticCostForClassKey(String classKey, String site) {
        return this.operatorSiteStaticCosts.get(classKey).get(site);
    }

    public int getPlatformStaticCostForClassKey(String classKey, String platform) {
        return this.operatorPlatformStaticCosts.get(classKey).get(platform);
    }

    //Stats API
    public Integer updateInputRateForOperator(String operator, int rate) {
        return this.inputRate.put(operator, rate);
    }

    public Integer updatePlatformStaticCostForOperator(String operator, String platform, int cost) {
        return this.operatorPlatformStaticCosts.get(operator).put(platform, cost);
    }

    public Integer updateSiteStaticCostForOperator(String operator, String site, int cost) {
        return this.operatorSiteStaticCosts.get(operator).put(site, cost);
    }

    public Integer updatePlatformMigrationCostForOperator(String operator, String platformFrom, String platformTo, int cost) {
        return this.operatorPlatformMigrationCosts.get(operator).get(platformFrom).put(platformTo, cost);
    }

    public Integer updateSiteMigrationCostForOperator(String operator, String siteFrom, String siteTo, int cost) {
        return this.operatorSiteMigrationCosts.get(operator).get(siteFrom).put(siteTo, cost);
    }

    public String getOriginalInput() {
        return originalInput;
    }

    // Add getter for operatorSiteStaticCosts
    public Map<String, Map<String, Integer>> getOperatorSiteStaticCosts() {
        return operatorSiteStaticCosts;
    }

    //Xenia added from exp22
    public Set<String> getOperators() {
        return this.operatorSiteStaticCosts.keySet();
    }

    @Override
    public String toString() {
        return "INFOREDictionary{" +
                "name='" + name + '\'' +
                ", costCoefficients=" + costCoefficients +
                ", inputRate=" + inputRate +
                ", operatorPlatformOperatorNames=" + operatorPlatformOperatorNames +
                ", operatorPlatformStaticCosts=" + operatorPlatformStaticCosts +
                ", operatorPlatformMigrationCosts=" + operatorPlatformMigrationCosts +
                ", operatorSiteStaticCosts=" + operatorSiteStaticCosts +
                ", operatorSiteMigrationCosts=" + operatorSiteMigrationCosts +
                '}';
    }

    public Map<String, Map<String, Map<String, Integer>>> getOperatorSiteMigrationCosts() {
        return operatorSiteMigrationCosts;
    }

    public Map<String, Map<String, String>> getOperatorPlatformOperatorNames() {
        return operatorPlatformOperatorNames;
    }

    public Map<String, Map<String, Integer>> getOperatorPlatformStaticCosts() {
        return operatorPlatformStaticCosts;
    }

    public Map<String, Map<String, Map<String, Integer>>> getOperatorPlatformMigrationCosts() {
        return operatorPlatformMigrationCosts;
    }
}

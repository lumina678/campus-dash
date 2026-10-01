package com.campusrelay.infrastructure.sharding;

import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableReferenceRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.algorithm.sharding.classbased.ClassBasedShardingAlgorithm;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampusRelayShardingRuleFactoryTest {

    @Test
    void createsShardingSphereRuleForTaskBindingTablesAndUserTables() {
        ShardingRuleConfiguration rule = CampusRelayShardingRuleFactory.create(4);

        Map<String, ShardingTableRuleConfiguration> tables = rule.getTables().stream()
                .collect(Collectors.toMap(ShardingTableRuleConfiguration::getLogicTable, Function.identity()));

        assertTaskTable(tables, "errand");
        assertTaskTable(tables, "grab_record");
        assertTaskTable(tables, "escrow_order");
        assertTaskTable(tables, "errand_status_log");

        assertUserTable(tables, "wallet_account");
        assertUserTable(tables, "wallet_ledger");
        assertUserTable(tables, "credit_score");
        assertUserTable(tables, "credit_event");

        assertTrue(rule.getBindingTableGroups().stream()
                .map(ShardingTableReferenceRuleConfiguration::getReference)
                .anyMatch("errand,grab_record,escrow_order,errand_status_log"::equals));
        assertEquals("CLASS_BASED", rule.getShardingAlgorithms().get("campus_relay_mod").getType());
        assertEquals(CampusRelayModuloShardingAlgorithm.class.getName(),
                rule.getShardingAlgorithms().get("campus_relay_mod").getProps().getProperty("algorithmClassName"));
    }

    @Test
    void classBasedAlgorithmCanInvokeCampusRelayModuloAlgorithm() {
        ShardingRuleConfiguration rule = CampusRelayShardingRuleFactory.create(4);
        ClassBasedShardingAlgorithm algorithm = new ClassBasedShardingAlgorithm();
        algorithm.init(rule.getShardingAlgorithms().get("campus_relay_mod").getProps());

        String target = algorithm.doSharding(
                List.of("campus_relay_0", "campus_relay_1", "campus_relay_2", "campus_relay_3"),
                new PreciseShardingValue<>("errand", "campus_id", null, 7L));

        assertEquals("campus_relay_3", target);
    }

    private static void assertTaskTable(Map<String, ShardingTableRuleConfiguration> tables, String tableName) {
        ShardingTableRuleConfiguration table = tables.get(tableName);

        assertEquals("campus_relay_${0..3}." + tableName, table.getActualDataNodes());
        StandardShardingStrategyConfiguration strategy =
                (StandardShardingStrategyConfiguration) table.getDatabaseShardingStrategy();
        assertEquals("campus_id", strategy.getShardingColumn());
        assertEquals("campus_relay_mod", strategy.getShardingAlgorithmName());
    }

    private static void assertUserTable(Map<String, ShardingTableRuleConfiguration> tables, String tableName) {
        ShardingTableRuleConfiguration table = tables.get(tableName);

        assertEquals("campus_relay_${0..3}." + tableName, table.getActualDataNodes());
        StandardShardingStrategyConfiguration strategy =
                (StandardShardingStrategyConfiguration) table.getDatabaseShardingStrategy();
        assertEquals("user_id", strategy.getShardingColumn());
        assertEquals("campus_relay_mod", strategy.getShardingAlgorithmName());
    }
}

package org.apache.zookeeper.server.metric;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class AvgMinMaxCounterSetTest {
    private AvgMinMaxCounterSet testCounterSet;

    @Before
    public void initCounter() {
        testCounterSet = new AvgMinMaxCounterSet("test");
    }

    private void addDataPoints() {
        testCounterSet.add("key1", 0);
        testCounterSet.add("key1", 1);
        testCounterSet.add("key2", 2);
        testCounterSet.add("key2", 3);
        testCounterSet.add("key2", 4);
        testCounterSet.add("key2", 5);
    }

    @Test
    public void testReset() {
        addDataPoints();
        testCounterSet.reset();

        Map<String, Object> values = testCounterSet.values();

        Assert.assertEquals("There should be 10 values in the set", 10, values.size());

        Assert.assertEquals("avg_key1_test should =0", 0D, values.get("avg_key1_test"));
        Assert.assertEquals("min_key1_test should =0", 0L, values.get("min_key1_test"));
        Assert.assertEquals("max_key1_test should =0", 0L, values.get("max_key1_test"));
        Assert.assertEquals("cnt_key1_test should =0", 0L, values.get("cnt_key1_test"));
        Assert.assertEquals("sum_key1_test should =0", 0L, values.get("sum_key1_test"));

        Assert.assertEquals("avg_key2_test should =0", 0D, values.get("avg_key2_test"));
        Assert.assertEquals("min_key2_test should =0", 0L, values.get("min_key2_test"));
        Assert.assertEquals("max_key2_test should =0", 0L, values.get("max_key2_test"));
        Assert.assertEquals("cnt_key2_test should =0", 0L, values.get("cnt_key2_test"));
        Assert.assertEquals("sum_key2_test should =0", 0L, values.get("sum_key2_test"));

    }

    @Test
    public void testValues() {
        addDataPoints();
        Map<String, Object> values = testCounterSet.values();

        Assert.assertEquals("There should be 10 values in the set", 10, values.size());
        Assert.assertEquals("avg_key1_test should =0.5", 0.5D, values.get("avg_key1_test"));
        Assert.assertEquals("min_key1_test should =0", 0L, values.get("min_key1_test"));
        Assert.assertEquals("max_key1_test should =1", 1L, values.get("max_key1_test"));
        Assert.assertEquals("cnt_key1_test should =2", 2L, values.get("cnt_key1_test"));
        Assert.assertEquals("sum_key1_test should =1", 1L, values.get("sum_key1_test"));

        Assert.assertEquals("avg_key2_test should =3.5", 3.5, values.get("avg_key2_test"));
        Assert.assertEquals("min_key2_test should =2", 2L, values.get("min_key2_test"));
        Assert.assertEquals("max_key2_test should =5", 5L, values.get("max_key2_test"));
        Assert.assertEquals("cnt_key2_test should =4", 4L, values.get("cnt_key2_test"));
        Assert.assertEquals("sum_key2_test should =14", 14L, values.get("sum_key2_test"));
    }
}

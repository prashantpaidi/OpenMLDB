package com._4paradigm.fesql_auto_test.checker;

import com._4paradigm.fesql.sqlcase.model.ExpectDesc;
import com._4paradigm.fesql.sqlcase.model.Table;
import com._4paradigm.fesql_auto_test.entity.FesqlResult;
import com._4paradigm.fesql_auto_test.util.FesqlUtil;
import com._4paradigm.sql.Schema;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;

import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author zhaowei
 * @date 2020/6/16 3:14 PM
 */
@Slf4j
public class ResultChecker extends BaseChecker {

    public ResultChecker(ExpectDesc expect, FesqlResult fesqlResult) {
        super(expect, fesqlResult);
    }

    @Override
    public void check() throws ParseException {
        log.info("result check");
        if (expect.getColumns().isEmpty()) {
            throw new RuntimeException("fail check result: columns are empty");
        }
        List<List<Object>> expectRows = FesqlUtil.convertRows(expect.getRows(),
                expect.getColumns());
        List<List<Object>> actual = fesqlResult.getResult();

        String orderName = expect.getOrder();
        if (orderName != null && orderName.length() > 0) {
            Schema schema = fesqlResult.getResultSchema();
            int index = 0;
            if (schema != null) {
                index = FesqlUtil.getIndexByColumnName(schema, orderName);
            } else {
                index = FesqlUtil.getIndexByColumnName(fesqlResult.getMetaData(), orderName);
            }
            Collections.sort(expectRows, new RowsSort(index));
            Collections.sort(actual, new RowsSort(index));
        }

        log.info("expect:{}", expectRows);
        log.info("actual:{}", actual);
        Assert.assertEquals(actual.size(), expectRows.size(),
                String.format("ResultChecker fail: expect size %d, real size %d", expectRows.size(), actual.size()));
        for (int i = 0; i < actual.size(); ++i) {
            List<Object> actual_list = actual.get(i);
            List<Object> expect_list = expectRows.get(i);
            Assert.assertEquals(actual_list.size(), expect_list.size(), String.format(
                    "ResultChecker fail at %dth row: expect row size %d, real row size %d",
                    i, expect_list.size(), actual_list.size()));
            for (int j = 0; j < actual_list.size(); ++j) {
                Object actual_val = actual_list.get(j);
                Object expect_val = expect_list.get(j);

                if (actual_val != null && actual_val instanceof Float) {
                    Assert.assertTrue(expect_val != null && expect_val instanceof Float);
                    Assert.assertEquals(
                            (Float) actual_val, (Float) expect_val, 1e-4,
                            String.format("ResultChecker fail: row=%d column=%d expect=%s real=%s\nexpect %s\nreal %s",
                                i, j, expect_val, actual_val,
                                Table.getTableString(expect.getColumns(), expectRows),
                                fesqlResult.toString())
                    );

                } else if (actual_val != null && actual_val instanceof Double) {
                    Assert.assertTrue(expect_val != null && expect_val instanceof Double);
                    Assert.assertEquals(
                            (Double) actual_val, (Double) expect_val, 1e-4,
                            String.format("ResultChecker fail: row=%d column=%d expect=%s real=%s\nexpect %s\nreal %s",
                                    i, j, expect_val, actual_val,
                                    Table.getTableString(expect.getColumns(), expectRows),
                                    fesqlResult.toString())
                    );

                } else {
                    Assert.assertEquals(actual_val, expect_val, String.format(
                            "ResultChecker fail: row=%d column=%d expect=%s real=%s\nexpect %s\nreal %s",
                            i, j, expect_val, actual_val,
                            Table.getTableString(expect.getColumns(), expectRows),
                            fesqlResult.toString()));

                }
            }
        }
    }

    public class RowsSort implements Comparator<List> {
        private int index;

        public RowsSort(int index) {
            this.index = index;
            if (-1 == index) {
                log.warn("compare without index");
            }
        }

        @Override
        public int compare(List o1, List o2) {
            if (-1 == index) {

                return 0;
            }
            Object obj1 = o1.get(index);
            Object obj2 = o2.get(index);
            if (obj1 == obj2) {
                return 0;
            }
            if (obj1 == null) {
                return -1;
            }
            if (obj2 == null) {
                return 1;
            }
            if (obj1 instanceof Comparable && obj2 instanceof Comparable) {
                return ((Comparable) obj1).compareTo(obj2);
            } else {
                return obj1.hashCode() - obj2.hashCode();
            }
        }
    }

}

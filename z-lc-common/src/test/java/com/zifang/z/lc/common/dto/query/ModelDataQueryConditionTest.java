package com.zifang.z.lc.common.dto.query;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModelDataQueryCondition 单元测试
 *
 * @author zifang
 */
class ModelDataQueryConditionTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        assertThat(condition).isNotNull();
    }

    @Test
    void shouldCreateWithParameterizedConstructor() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition(ModelDataQueryCondition.TYPE_AND);
        assertThat(condition.getType()).isEqualTo(ModelDataQueryCondition.TYPE_AND);
    }

    @Test
    void shouldSetAndGetKey() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        condition.setKey("condition-001");
        assertThat(condition.getKey()).isEqualTo("condition-001");
    }

    @Test
    void shouldSetAndGetLevel() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        condition.setLevel(2);
        assertThat(condition.getLevel()).isEqualTo(2);
    }

    @Test
    void shouldSetAndGetType() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        condition.setType(ModelDataQueryCondition.TYPE_OR);
        assertThat(condition.getType()).isEqualTo(ModelDataQueryCondition.TYPE_OR);
    }

    @Test
    void shouldSetAndGetRowValues() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        ModelDataQueryCondition.FieldCondition fieldCondition = new ModelDataQueryCondition.FieldCondition("field1", "=", "value1");
        condition.setRowValues(fieldCondition);
        assertThat(condition.getRowValues()).isEqualTo(fieldCondition);
    }

    @Test
    void shouldSetAndGetChildren() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        ModelDataQueryCondition child = new ModelDataQueryCondition();
        List<ModelDataQueryCondition> children = Arrays.asList(child);
        condition.setChildren(children);
        assertThat(condition.getChildren()).isEqualTo(children);
    }

    @Test
    void shouldHandleNullValues() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        assertThat(condition.getKey()).isNull();
        assertThat(condition.getLevel()).isNull();
        assertThat(condition.getType()).isNull();
        assertThat(condition.getRowValues()).isNull();
        assertThat(condition.getChildren()).isNull();
    }

    @Test
    void shouldImplementSerializable() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        assertThat(condition).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void shouldHaveCorrectConstants() {
        assertThat(ModelDataQueryCondition.TYPE_AND).isEqualTo(1);
        assertThat(ModelDataQueryCondition.TYPE_OR).isEqualTo(2);
    }

    @Test
    void shouldAddChildCondition() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        ModelDataQueryCondition child = new ModelDataQueryCondition();
        condition.addChild(child);
        
        assertThat(condition.getChildren()).hasSize(1);
        assertThat(condition.getChildren().get(0)).isEqualTo(child);
    }

    @Test
    void shouldAddMultipleChildConditions() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        ModelDataQueryCondition child1 = new ModelDataQueryCondition();
        ModelDataQueryCondition child2 = new ModelDataQueryCondition();
        condition.addChild(child1);
        condition.addChild(child2);
        
        assertThat(condition.getChildren()).hasSize(2);
        assertThat(condition.getChildren().get(0)).isEqualTo(child1);
        assertThat(condition.getChildren().get(1)).isEqualTo(child2);
    }

    @Test
    void shouldAddLeafCondition() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        ModelDataQueryCondition.FieldCondition leaf = new ModelDataQueryCondition.FieldCondition("field1", "=", "value1");
        condition.addChild(leaf);
        
        assertThat(condition.getChildren()).hasSize(1);
        assertThat(condition.getChildren().get(0).getRowValues()).isEqualTo(leaf);
    }

    @Test
    void shouldCreateEqConditionWithFactoryMethod() {
        ModelDataQueryCondition condition = ModelDataQueryCondition.eq("field1", "value1");
        assertThat(condition.getRowValues()).isNotNull();
        assertThat(condition.getRowValues().getParamKey()).isEqualTo("field1");
        assertThat(condition.getRowValues().getRule()).isEqualTo("=");
        assertThat(condition.getRowValues().getParamValue()).isEqualTo("value1");
    }

    @Test
    void shouldCreateLikeConditionWithFactoryMethod() {
        ModelDataQueryCondition condition = ModelDataQueryCondition.like("field1", "value1");
        assertThat(condition.getRowValues()).isNotNull();
        assertThat(condition.getRowValues().getParamKey()).isEqualTo("field1");
        assertThat(condition.getRowValues().getRule()).isEqualTo("like");
        assertThat(condition.getRowValues().getParamValue()).isEqualTo("value1");
    }

    @Test
    void shouldCreateEqConditionWithNullValue() {
        ModelDataQueryCondition condition = ModelDataQueryCondition.eq("field1", null);
        assertThat(condition.getRowValues()).isNotNull();
        assertThat(condition.getRowValues().getParamKey()).isEqualTo("field1");
        assertThat(condition.getRowValues().getRule()).isEqualTo("=");
        assertThat(condition.getRowValues().getParamValue()).isNull();
    }

    @Test
    void shouldCreateLikeConditionWithNullValue() {
        ModelDataQueryCondition condition = ModelDataQueryCondition.like("field1", null);
        assertThat(condition.getRowValues()).isNotNull();
        assertThat(condition.getRowValues().getParamKey()).isEqualTo("field1");
        assertThat(condition.getRowValues().getRule()).isEqualTo("like");
        assertThat(condition.getRowValues().getParamValue()).isNull();
    }

    @Test
    void shouldSetEmptyChildren() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        condition.setChildren(Arrays.asList());
        assertThat(condition.getChildren()).isEmpty();
    }

    @Test
    void shouldSetNullChildren() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        condition.setChildren(null);
        assertThat(condition.getChildren()).isNull();
    }

    @Test
    void shouldSetEmptyKey() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        condition.setKey("");
        assertThat(condition.getKey()).isEmpty();
    }

    @Test
    void shouldSetNullKey() {
        ModelDataQueryCondition condition = new ModelDataQueryCondition();
        condition.setKey(null);
        assertThat(condition.getKey()).isNull();
    }
}
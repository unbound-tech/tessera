package com.quorum.tessera.config.adapters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;

public class MapAdapterTest {

    private MapAdapter adapter;

    @Before
    public void onSetup() {
        adapter = new MapAdapter();
    }

    @Test
    public void marshalEmpty() throws Exception {

        Map<String, String> map = new HashMap<>();

        MapAdapter.ConfigProperties outcome = adapter.marshal(map);

        assertThat(outcome.getProperties()).isEmpty();

    }

    @Test
    public void marshalNull() throws Exception {
        assertThat(adapter.marshal(null)).isNull();
    }

    @Test
    public void marshal() throws Exception {

        Map<String, String> map = new LinkedHashMap<>();
        map.put("message", "I love sparrows!!");
        map.put("greeting", "Hellow");

        MapAdapter.ConfigProperties outcome = adapter.marshal(map);

        assertThat(outcome.getProperties()).hasSize(2);

        List<String> names = outcome.getProperties().stream()
                .map(JAXBElement::getName)
                .map(QName::getLocalPart)
                .collect(Collectors.toList());

        assertThat(names).containsExactly("message", "greeting");

        List<String> values = outcome.getProperties().stream()
                .map(JAXBElement::getValue)
                .collect(Collectors.toList());

        assertThat(values).containsExactly("I love sparrows!!", "Hellow");

    }

    @Test
    public void unmarshal() throws Exception {

        MapAdapter.ConfigProperties properties = new MapAdapter.ConfigProperties();

        JAXBElement<String> someElement = new JAXBElement<>(QName.valueOf("message"), String.class, "I love sparrows!!");
        JAXBElement<String> someOtherElement = new JAXBElement<>(QName.valueOf("greeting"), String.class, "Hellow");

        properties.setProperties(Arrays.asList(someElement, someOtherElement));

        Map<String, String> result = adapter.unmarshal(properties);

        Map<String, String> map = new LinkedHashMap<>();
        map.put("message", "I love sparrows!!");
        map.put("greeting", "Hellow");

        assertThat(result).containsAllEntriesOf(map);

    }

    @Test
    public void unmarshalNull() throws Exception {
        assertThat(adapter.unmarshal(null)).isNull();
    }
    
        @Test
    public void unmarshalEmpty() throws Exception {
        assertThat(adapter.unmarshal(new MapAdapter.ConfigProperties())).isEmpty();
    }

}

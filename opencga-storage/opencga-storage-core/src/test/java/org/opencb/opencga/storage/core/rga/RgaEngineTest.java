package org.opencb.opencga.storage.core.rga;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opencb.commons.datastore.core.DataResult;
import org.opencb.commons.datastore.core.FacetField;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.core.common.JacksonUtils;
import org.opencb.opencga.core.models.analysis.knockout.*;
import org.opencb.opencga.core.response.OpenCGAResult;
import org.opencb.opencga.storage.core.config.StorageConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.opencb.opencga.storage.core.rga.RgaUtilsTest.createKnockoutByIndividual;

public class RgaEngineTest {

    private StorageConfiguration storageConfiguration;

    @Rule
    public RgaSolrExtenalResource solr = new RgaSolrExtenalResource();

    @Before
    public void before() throws IOException {
        try (InputStream is = RgaEngineTest.class.getClassLoader().getResourceAsStream("storage-configuration.yml")) {
            storageConfiguration = StorageConfiguration.load(is);
        }
    }

    @Test
    public void testIndividualQuery() throws Exception {
        RgaEngine rgaEngine = solr.configure(storageConfiguration);

        String collection = solr.coreName;
        rgaEngine.create(collection);

        List<KnockoutByIndividual> knockoutByIndividualList = new ArrayList<>(2);
        knockoutByIndividualList.add(createKnockoutByIndividual(1));
        knockoutByIndividualList.add(createKnockoutByIndividual(2));

        rgaEngine.insert(collection, knockoutByIndividualList);
        OpenCGAResult<KnockoutByIndividual> result = rgaEngine.individualQuery(collection, new Query(), new QueryOptions());

        assertEquals(2, result.getNumResults());
        for (int i = 0; i < knockoutByIndividualList.size(); i++) {
            assertEquals(JacksonUtils.getDefaultObjectMapper().writeValueAsString(knockoutByIndividualList.get(i)),
                    JacksonUtils.getDefaultObjectMapper().writeValueAsString(result.getResults().get(i)));
        }

        result = rgaEngine.individualQuery(collection, new Query(), new QueryOptions());
        assertEquals(2, result.getNumResults());

        Query query = new Query(RgaQueryParams.DISORDERS.key(), "disorderId1");
        result = rgaEngine.individualQuery(collection, query, new QueryOptions());
        assertEquals(1, result.getNumResults());
        assertEquals("id1", result.first().getId());

        query = new Query(RgaQueryParams.DISORDERS.key(), "disorderId2");
        result = rgaEngine.individualQuery(collection, query, new QueryOptions());
        assertEquals(1, result.getNumResults());
        assertEquals("id2", result.first().getId());

        query = new Query(RgaQueryParams.DISORDERS.key(), "disorderId6");
        result = rgaEngine.individualQuery(collection, query, new QueryOptions());
        assertEquals(0, result.getNumResults());

        query = new Query(RgaQueryParams.CONSEQUENCE_TYPE.key(), "SO:0001891");
        result = rgaEngine.individualQuery(collection, query, new QueryOptions());
        assertEquals(2, result.getNumResults());

        query = new Query(RgaQueryParams.CONSEQUENCE_TYPE.key(), "SO:0001822");
        result = rgaEngine.individualQuery(collection, query, new QueryOptions());
        assertEquals(0, result.getNumResults());

        query = new Query()
                .append(RgaQueryParams.DISORDERS.key(), "disorderId1")
                .append(RgaQueryParams.CONSEQUENCE_TYPE.key(), "SO:0001891");
        result = rgaEngine.individualQuery(collection, query, new QueryOptions());
        assertEquals(1, result.getNumResults());

        query = new Query()
                .append(RgaQueryParams.DISORDERS.key(), "disorderId1,disorder")
                .append(RgaQueryParams.CONSEQUENCE_TYPE.key(), "SO:0001891");
        result = rgaEngine.individualQuery(collection, query, new QueryOptions());
        assertEquals(1, result.getNumResults());
    }

    @Test
    public void testIncludeExcludeIndividualQuery() throws Exception {
        RgaEngine rgaEngine = solr.configure(storageConfiguration);

        String collection = solr.coreName;
        rgaEngine.create(collection);

        List<KnockoutByIndividual> knockoutByIndividualList = new ArrayList<>(2);
        knockoutByIndividualList.add(createKnockoutByIndividual(1));
        knockoutByIndividualList.add(createKnockoutByIndividual(2));

        rgaEngine.insert(collection, knockoutByIndividualList);

        QueryOptions options = new QueryOptions(QueryOptions.INCLUDE, Arrays.asList("sampleId", "disorders", "genesMap.name"));
        OpenCGAResult<KnockoutByIndividual> result = rgaEngine.individualQuery(collection, new Query(), options);
        assertEquals(2, result.getNumResults());
        for (int i = 0; i < knockoutByIndividualList.size(); i++) {
            assertNotNull(result.getResults().get(i).getId());
            assertTrue(result.getResults().get(i).getPhenotypes().isEmpty());
            assertNotNull(result.getResults().get(i).getSampleId());
            assertNotNull(result.getResults().get(i).getDisorders());
            assertNotNull(result.getResults().get(i).getGenes());
            for (KnockoutByIndividual.KnockoutGene gene : result.getResults().get(i).getGenes()) {
                assertNotNull(gene.getId());
                assertNotNull(gene.getName());
                assertNull(gene.getStrand());
                assertTrue(gene.getTranscripts().isEmpty());
            }
        }

        options = new QueryOptions(QueryOptions.EXCLUDE, Arrays.asList("sampleId", "disorders", "genesMap.name",
                "genesMap.transcriptsMap.variants"));
        result = rgaEngine.individualQuery(collection, new Query(), options);
        assertEquals(2, result.getNumResults());
        for (int i = 0; i < knockoutByIndividualList.size(); i++) {
            assertNotNull(result.getResults().get(i).getId());
            assertFalse(result.getResults().get(i).getPhenotypes().isEmpty());
            assertNull(result.getResults().get(i).getSampleId());
            assertTrue(result.getResults().get(i).getDisorders().isEmpty());
            assertNotNull(result.getResults().get(i).getGenes());
            for (KnockoutByIndividual.KnockoutGene gene : result.getResults().get(i).getGenes()) {
                assertNotNull(gene.getId());
                assertNull(gene.getName());
//                assertNotNull(gene.getStrand());
                assertFalse(gene.getTranscripts().isEmpty());
                for (KnockoutTranscript transcript : gene.getTranscripts()) {
                    assertNotNull(transcript.getId());
                    assertNotNull(transcript.getBiotype());
                    assertTrue(transcript.getVariants().isEmpty());
                }
            }
        }

        // It should be not possible to completely exclude knockoutType without excluding the whole variant information
        options = new QueryOptions(QueryOptions.EXCLUDE, Arrays.asList("sampleId", "disorders", "genesMap.name",
                "genesMap.transcriptsMap.variants.knockoutType"));
        result = rgaEngine.individualQuery(collection, new Query(), options);
        assertEquals(2, result.getNumResults());
        for (int i = 0; i < knockoutByIndividualList.size(); i++) {
            assertNotNull(result.getResults().get(i).getId());
            assertFalse(result.getResults().get(i).getPhenotypes().isEmpty());
            assertNull(result.getResults().get(i).getSampleId());
            assertTrue(result.getResults().get(i).getDisorders().isEmpty());
            assertNotNull(result.getResults().get(i).getGenes());
            for (KnockoutByIndividual.KnockoutGene gene : result.getResults().get(i).getGenes()) {
                assertNotNull(gene.getId());
                assertNull(gene.getName());
//                assertNotNull(gene.getStrand());
                assertFalse(gene.getTranscripts().isEmpty());
                for (KnockoutTranscript transcript : gene.getTranscripts()) {
                    assertNotNull(transcript.getId());
                    assertNotNull(transcript.getBiotype());
                    assertFalse(transcript.getVariants().isEmpty());
                    for (KnockoutVariant variant : transcript.getVariants()) {
                        assertNotNull(variant.getId());
                        assertNotNull(variant.getKnockoutType());
                        assertFalse(variant.getPopulationFrequencies().isEmpty());
                    }
                }
            }
        }

    }

    @Test
    public void testGeneQuery() throws Exception {
        RgaEngine rgaEngine = solr.configure(storageConfiguration);

        String collection = solr.coreName;
        rgaEngine.create(collection);

        List<KnockoutByIndividual> knockoutByIndividualList = new ArrayList<>(2);
        knockoutByIndividualList.add(createKnockoutByIndividual(1));
        knockoutByIndividualList.add(createKnockoutByIndividual(2));

        rgaEngine.insert(collection, knockoutByIndividualList);
        OpenCGAResult<RgaKnockoutByGene> result = rgaEngine.geneQuery(collection, new Query(), new QueryOptions());

        assertEquals(4, result.getNumResults());
        for (RgaKnockoutByGene resultResult : result.getResults()) {
            assertEquals(1, resultResult.getIndividuals().size());
            assertTrue(resultResult.getIndividuals().get(0).getId().equals("id1")
                    || resultResult.getIndividuals().get(0).getId().equals("id2"));
        }

        Query query = new Query(RgaQueryParams.DISORDERS.key(), "disorderId1");
        result = rgaEngine.geneQuery(collection, query, new QueryOptions());
        assertEquals(2, result.getNumResults());
        for (RgaKnockoutByGene resultResult : result.getResults()) {
            assertEquals(1, resultResult.getIndividuals().size());
            assertEquals("id1", resultResult.getIndividuals().get(0).getId());
        }

        query = new Query(RgaQueryParams.DISORDERS.key(), "disorderId2");
        result = rgaEngine.geneQuery(collection, query, new QueryOptions());
        assertEquals(2, result.getNumResults());
        for (RgaKnockoutByGene resultResult : result.getResults()) {
            assertEquals(1, resultResult.getIndividuals().size());
            assertEquals("id2", resultResult.getIndividuals().get(0).getId());
        }

        query = new Query(RgaQueryParams.DISORDERS.key(), "disorderId6");
        result = rgaEngine.geneQuery(collection, query, new QueryOptions());
        assertEquals(0, result.getNumResults());

        query = new Query(RgaQueryParams.CONSEQUENCE_TYPE.key(), "SO:0001891");
        result = rgaEngine.geneQuery(collection, query, new QueryOptions());
        assertEquals(4, result.getNumResults());
        for (RgaKnockoutByGene resultResult : result.getResults()) {
            assertEquals(1, resultResult.getIndividuals().size());
            assertTrue(resultResult.getIndividuals().get(0).getId().equals("id1")
                    || resultResult.getIndividuals().get(0).getId().equals("id2"));
        }

        query = new Query(RgaQueryParams.CONSEQUENCE_TYPE.key(), "SO:0001822");
        result = rgaEngine.geneQuery(collection, query, new QueryOptions());
        assertEquals(0, result.getNumResults());

        query = new Query()
                .append(RgaQueryParams.DISORDERS.key(), "disorderId1")
                .append(RgaQueryParams.CONSEQUENCE_TYPE.key(), "SO:0001891");
        result = rgaEngine.geneQuery(collection, query, new QueryOptions());
        assertEquals(2, result.getNumResults());

        query = new Query()
                .append(RgaQueryParams.DISORDERS.key(), "disorderId1,disorder")
                .append(RgaQueryParams.CONSEQUENCE_TYPE.key(), "SO:0001891");
        result = rgaEngine.geneQuery(collection, query, new QueryOptions());
        assertEquals(2, result.getNumResults());
    }

    @Test
    public void testIncludeExcludeGeneQuery() throws Exception {
        RgaEngine rgaEngine = solr.configure(storageConfiguration);

        String collection = solr.coreName;
        rgaEngine.create(collection);

        List<KnockoutByIndividual> knockoutByIndividualList = new ArrayList<>(2);
        knockoutByIndividualList.add(createKnockoutByIndividual(1));
        knockoutByIndividualList.add(createKnockoutByIndividual(2));

        rgaEngine.insert(collection, knockoutByIndividualList);

        QueryOptions options = new QueryOptions(QueryOptions.INCLUDE, Arrays.asList("name", "individuals.transcriptsMap.chromosome"));
        OpenCGAResult<RgaKnockoutByGene> result = rgaEngine.geneQuery(collection, new Query(), options);
        assertEquals(4, result.getNumResults());
        for (RgaKnockoutByGene gene : result.getResults()) {
            assertNotNull(gene.getId());
            assertNotNull(gene.getName());
            assertNull(gene.getBiotype());
            for (RgaKnockoutByGene.KnockoutIndividual individual : gene.getIndividuals()) {
                assertNotNull(individual.getId());
                assertNull(individual.getSampleId());
                for (KnockoutTranscript transcript : individual.getTranscripts()) {
                    assertNotNull(transcript.getId());
//                    assertNotNull(transcript.getChromosome());
                    assertNull(transcript.getBiotype());
                    assertTrue(transcript.getVariants().isEmpty());
                }
            }
        }

        options = new QueryOptions(QueryOptions.EXCLUDE, Arrays.asList("name", "individuals.transcriptsMap.chromosome",
                "individuals.transcriptsMap.variants"));
        result = rgaEngine.geneQuery(collection, new Query(), options);
        assertEquals(4, result.getNumResults());
        for (RgaKnockoutByGene gene : result.getResults()) {
            assertNotNull(gene.getId());
            assertNull(gene.getName());
//            assertNotNull(gene.getBiotype());
            for (RgaKnockoutByGene.KnockoutIndividual individual : gene.getIndividuals()) {
                assertNotNull(individual.getId());
                assertNotNull(individual.getSampleId());
                for (KnockoutTranscript transcript : individual.getTranscripts()) {
                    assertNotNull(transcript.getId());
                    assertNull(transcript.getChromosome());
                    assertNotNull(transcript.getBiotype());
                    assertTrue(transcript.getVariants().isEmpty());
                }
            }
        }

        // It should be not possible to completely exclude knockoutType without excluding the whole variant information
        options = new QueryOptions(QueryOptions.EXCLUDE, Arrays.asList("name", "individuals.transcriptsMap.chromosome",
                "individuals.transcriptsMap.variants.knockoutType"));
        result = rgaEngine.geneQuery(collection, new Query(), options);
        assertEquals(4, result.getNumResults());
        for (RgaKnockoutByGene gene : result.getResults()) {
            assertNotNull(gene.getId());
            assertNull(gene.getName());
//            assertNotNull(gene.getBiotype());
            for (RgaKnockoutByGene.KnockoutIndividual individual : gene.getIndividuals()) {
                assertNotNull(individual.getId());
                assertNotNull(individual.getSampleId());
                for (KnockoutTranscript transcript : individual.getTranscripts()) {
                    assertNotNull(transcript.getId());
                    assertNull(transcript.getChromosome());
                    assertNotNull(transcript.getBiotype());
                    assertFalse(transcript.getVariants().isEmpty());
                    for (KnockoutVariant variant : transcript.getVariants()) {
                        assertNotNull(variant.getId());
                        assertNotNull(variant.getKnockoutType());
                        assertFalse(variant.getPopulationFrequencies().isEmpty());
                    }
                }
            }
        }
    }

    @Test
    public void testVariantQuery() throws Exception {
        RgaEngine rgaEngine = solr.configure(storageConfiguration);

        String collection = solr.coreName;
        rgaEngine.create(collection);

        List<KnockoutByIndividual> knockoutByIndividualList = new ArrayList<>(2);
        knockoutByIndividualList.add(createKnockoutByIndividual(1));
        knockoutByIndividualList.add(createKnockoutByIndividual(2));

        rgaEngine.insert(collection, knockoutByIndividualList);
        OpenCGAResult<KnockoutByVariant> result = rgaEngine.variantQuery(collection, new Query(), new QueryOptions());

        assertEquals(6, result.getNumResults());
    }

    @Test
    public void testIncludeExcludeVariantQuery() throws Exception {
        RgaEngine rgaEngine = solr.configure(storageConfiguration);

        String collection = solr.coreName;
        rgaEngine.create(collection);

        List<KnockoutByIndividual> knockoutByIndividualList = new ArrayList<>(2);
        knockoutByIndividualList.add(createKnockoutByIndividual(1));
        knockoutByIndividualList.add(createKnockoutByIndividual(2));

        rgaEngine.insert(collection, knockoutByIndividualList);

        QueryOptions options = new QueryOptions(QueryOptions.INCLUDE, Arrays.asList("individuals.sampleId", "individuals.disorders",
                "individuals.genesMap.name"));
        OpenCGAResult<KnockoutByVariant> result = rgaEngine.variantQuery(collection, new Query(), options);
        assertEquals(6, result.getNumResults());
        for (KnockoutByVariant variant : result.getResults()) {
            assertNotNull(variant.getId());
            for (KnockoutByIndividual individual : variant.getIndividuals()) {
                assertNotNull(individual.getId());
                assertTrue(individual.getPhenotypes().isEmpty());
                assertNotNull(individual.getSampleId());
                assertNotNull(individual.getDisorders());
                assertNotNull(individual.getGenes());
                for (KnockoutByIndividual.KnockoutGene gene : individual.getGenes()) {
                    assertNotNull(gene.getId());
                    assertNotNull(gene.getName());
                    assertNull(gene.getStrand());
                    assertFalse(gene.getTranscripts().isEmpty());
                }

            }
        }

        // Not possible excluding variants object because that's basic for this data model
        options = new QueryOptions(QueryOptions.EXCLUDE, Arrays.asList("individuals.sampleId", "individuals.disorders",
                "individuals.genesMap.name", "individuals.genesMap.transcriptsMap.variants"));
        result = rgaEngine.variantQuery(collection, new Query(), options);
        assertEquals(6, result.getNumResults());
        for (KnockoutByVariant variant : result.getResults()) {
            assertNotNull(variant.getId());
            for (KnockoutByIndividual individual : variant.getIndividuals()) {
                assertNotNull(individual.getId());
                assertFalse(individual.getPhenotypes().isEmpty());
                assertNull(individual.getSampleId());
                assertTrue(individual.getDisorders().isEmpty());
                assertNotNull(individual.getGenes());
                for (KnockoutByIndividual.KnockoutGene gene : individual.getGenes()) {
                    assertNotNull(gene.getId());
                    assertNull(gene.getName());
//                assertNotNull(gene.getStrand());
                    assertFalse(gene.getTranscripts().isEmpty());
                    for (KnockoutTranscript transcript : gene.getTranscripts()) {
                        assertNotNull(transcript.getId());
                        assertNotNull(transcript.getBiotype());
                        assertFalse(transcript.getVariants().isEmpty());
                    }

                }
            }
        }

        // It should be not possible to completely exclude knockoutType without excluding the whole variant information
        options = new QueryOptions(QueryOptions.EXCLUDE, Arrays.asList("individuals.sampleId", "individuals.disorders",
                "individuals.genesMap.name", "individuals.genesMap.transcriptsMap.variants.knockoutType"));
        result = rgaEngine.variantQuery(collection, new Query(), options);
        assertEquals(6, result.getNumResults());
        for (KnockoutByVariant variant : result.getResults()) {
            assertNotNull(variant.getId());
            for (KnockoutByIndividual individual : variant.getIndividuals()) {
                assertNotNull(individual.getId());
                assertFalse(individual.getPhenotypes().isEmpty());
                assertNull(individual.getSampleId());
                assertTrue(individual.getDisorders().isEmpty());
                assertNotNull(individual.getGenes());
                for (KnockoutByIndividual.KnockoutGene gene : individual.getGenes()) {
                    assertNotNull(gene.getId());
                    assertNull(gene.getName());
//                assertNotNull(gene.getStrand());
                    assertFalse(gene.getTranscripts().isEmpty());
                    for (KnockoutTranscript transcript : gene.getTranscripts()) {
                        assertNotNull(transcript.getId());
                        assertNotNull(transcript.getBiotype());
                        assertFalse(transcript.getVariants().isEmpty());
                        for (KnockoutVariant tmpVariant : transcript.getVariants()) {
                            assertNotNull(tmpVariant.getId());
                            assertNotNull(tmpVariant.getKnockoutType());
                            assertFalse(tmpVariant.getPopulationFrequencies().isEmpty());
                        }
                    }
                }
            }
        }

    }


    @Test
    public void testFacet() throws Exception {
        RgaEngine rgaEngine = solr.configure(storageConfiguration);

        String collection = solr.coreName;
        rgaEngine.create(collection);

        List<KnockoutByIndividual> knockoutByIndividualList = new ArrayList<>(2);
        knockoutByIndividualList.add(createKnockoutByIndividual(1));
        knockoutByIndividualList.add(createKnockoutByIndividual(2));

        rgaEngine.insert(collection, knockoutByIndividualList);

        QueryOptions options = new QueryOptions(QueryOptions.FACET, RgaQueryParams.DISORDERS.key());
        DataResult<FacetField> facetFieldDataResult = rgaEngine.facetedQuery(collection, new Query(), options);
        assertEquals(1, facetFieldDataResult.getNumResults());
        assertEquals(RgaDataModel.DISORDERS, facetFieldDataResult.first().getName());
        assertEquals(4, facetFieldDataResult.first().getBuckets().size());
    }

}

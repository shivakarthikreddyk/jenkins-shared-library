package org.sakila

class PipelineConfig implements Serializable {

    String dockerUser
    String backendImage
    String frontendImage
    String namespace

    PipelineConfig() {

        dockerUser = ''

        backendImage = ''

        frontendImage = ''

        namespace = 'sakila'
    }
}
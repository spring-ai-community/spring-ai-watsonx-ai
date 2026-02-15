import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: 'Enterprise-Ready AI Models',
    description: (
      <>
        Access IBM Granite, Meta Llama, Mistral AI, and other production-ready foundation models.
        Built for enterprise scale with IBM's trusted AI platform.
      </>
    ),
  },
  {
    title: 'Comprehensive AI Capabilities',
    description: (
      <>
        Chat models for conversational AI, embeddings for semantic search, content moderation
        with HAP/PII/Granite Guardian detectors, and document reranking for RAG pipelines.
      </>
    ),
  },
  {
    title: 'Spring Boot Integration',
    description: (
      <>
        Zero-configuration setup with Spring Boot auto-configuration. Familiar Spring AI abstractions,
        reactive support with WebFlux, and function calling for tool integration.
      </>
    ),
  },
];

function Feature({title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className={styles.featureCard}>
        <Heading as="h3" className={styles.featureTitle}>{title}</Heading>
        <p className={styles.featureDescription}>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}

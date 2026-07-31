import type {ReactNode} from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Translate from '@docusaurus/Translate';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';

import styles from './index.module.css';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <Heading as="h1" className="hero__title">
          {siteConfig.title}
        </Heading>
        <p className="hero__subtitle">{siteConfig.tagline}</p>
        <div className={styles.buttons}>
          <Link className="button button--secondary button--lg" to="/docs/quick-start">
            <Translate id="home.cta">Get Started</Translate>
          </Link>
        </div>
      </div>
    </header>
  );
}

const features: Array<{title: ReactNode; description: ReactNode}> = [
  {
    title: <Translate id="home.feature.monitor.title">Monitors your machines</Translate>,
    description: (
      <Translate id="home.feature.monitor.description">
        Watches your machines and notifies you on Discord when something happens:
        your World Eater stops, your Trencher stops breaking blocks, your Bedrock
        Breaker breaks.
      </Translate>
    ),
  },
  {
    title: <Translate id="home.feature.machineTypes.title">Three machine types</Translate>,
    description: (
      <Translate id="home.feature.machineTypes.description">
        World Eater (TNT count detection), Trencher and Bedrock Breaker (explosion
        block-destruction detection).
      </Translate>
    ),
  },
  {
    title: <Translate id="home.feature.delivery.title">Two Discord delivery modes</Translate>,
    description: (
      <Translate id="home.feature.delivery.description">
        Webhook mode (default) — simple HTTP webhook, no bot needed. Bot mode —
        Discord bot with JDA including slash commands and an interactive
        "Toggle Ping" button.
      </Translate>
    ),
  },
];

export default function Home(): ReactNode {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={siteConfig.title}
      description={siteConfig.tagline}>
      <HomepageHeader />
      <main>
        <section className={styles.features}>
          <div className="container">
            <div className="row">
              {features.map((f, idx) => (
                <div key={idx} className={clsx('col col--4')}>
                  <div className="text--center padding-horiz--md">
                    <Heading as="h3">{f.title}</Heading>
                    <p>{f.description}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>
    </Layout>
  );
}

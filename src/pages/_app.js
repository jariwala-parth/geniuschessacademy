import '../styles/globals.css';
import { ThemeProvider } from '../context/ThemeContext';
import { AuthProvider } from '../context/AuthContext';
import Head from 'next/head';
import { useEffect } from 'react';

function MyApp({ Component, pageProps }) {
  // Security enhancement to mitigate server action vulnerabilities
  useEffect(() => {
    // Disable potential dangerous features when not needed
    if (typeof window !== 'undefined') {
      window.__NEXT_DISABLE_ACTIONS__ = true;
    }
  }, []);

  return (
    <>
      <Head>
        <title>Genius Chess Academy</title>
        <meta name="description" content="Learn and play chess with the best coaches" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <meta httpEquiv="Content-Security-Policy" content="default-src 'self'; img-src 'self' data:; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';" />
        <link rel="icon" href="/favicon.ico" />
      </Head>
      <AuthProvider>
        <ThemeProvider>
          <Component {...pageProps} />
        </ThemeProvider>
      </AuthProvider>
    </>
  );
}

export default MyApp; 
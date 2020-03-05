import React from 'react';
import logo from './logo.svg';
import './App.css';

function App() {
  return (
    <div>
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
      </header>
      <div className="App">
        <p>
            EnMasse Sandbox is a community service offering for developers who want to try EnMasse without setting up Kubernetes and installing Enmasse.
        </p>
        <p>
            You can use a <a href="https://www.github.com">GitHub</a> account to register and get an environment created for 7 days. Once expired, all data for the environment and account data will be deleted.
        </p>
        <a className="link" href="/register">Register</a>
        &nbsp;
        &nbsp;
        &nbsp;
        &nbsp;
        <a className="link" href="/dashboard">Dashboard</a>
      </div>
    </div>
  );
}

export default App;

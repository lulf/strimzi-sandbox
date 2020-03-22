import React, { Component } from 'react';
import logo from './logo.svg';
import './App.css';

class Header extends Component {
  render() {
      return (
              <div>
        <header className="App-header">
            <img src={logo} className="App-logo" alt="logo" />
        </header>
        <div className="App">
            <p>
                EnMasse Sandbox is a public service for developers who want to try a managed EnMasse service without having to setting up Kubernetes.
            </p>
            <p>
                You can use a <a href="https://www.github.com">GitHub</a> account to register and get a free environment for a limited time period. Once expired, all data for the environment and account data will be deleted.
            </p>
        </div></div>);
  }
};

export default Header;

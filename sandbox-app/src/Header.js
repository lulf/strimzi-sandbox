import React, { Component } from 'react';
import logo from './logo.png';
import './App.css';

class Header extends Component {
  render() {
      return (
        <div className="App">
            <img src={logo} className="App-logo" alt="logo" />
            <p>
                EnMasse Sandbox is a public service for developers who want to try a managed EnMasse service without having to setting up Kubernetes.
            </p>
            <p>
                You can use a <a href="https://www.github.com">GitHub</a> account to register and get a free environment for a limited time period. Once expired, all data for the environment and account data will be deleted.
            </p>
        </div>);
  }
};

export default Header;

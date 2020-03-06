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
                EnMasse Sandbox is a community service offering for developers who want to try EnMasse without setting up Kubernetes and installing Enmasse.
            </p>
            <p>
                You can use a <a href="https://www.github.com">GitHub</a> account to register and get an environment created for 7 days. Once expired, all data for the environment and account data will be deleted.
            </p>
        </div></div>);
  }
};

export default Header;

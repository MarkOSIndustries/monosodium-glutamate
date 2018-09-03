#!/usr/bin/env node
const actions = require('./actions');

const [nodePath, selfPath, ...args] = process.argv;

const shortcuts = {
  e: 'encode',
  d: 'decode',
};

if(!args.length) {
  actions.usage();
  return;
}

const [action, ...actionArgs] = args;

if(shortcuts.hasOwnProperty(action)) {
  action = shortcuts[action];
}

if(!actions.hasOwnProperty(action)) {
  console.log(`Action '${action}' not supported.`);
  action = usage;
}

actions[action](...actionArgs);
